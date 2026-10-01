package com.detoxmate.friend;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.common.error.GlobalExceptionHandlerTestFixture;
import com.detoxmate.friend.controller.FriendController;
import com.detoxmate.friend.repository.FriendRepository;
import com.detoxmate.friend.service.FriendService;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.repository.UserRepository;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FriendFlowIntegrationTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRepository friendRepository;

    private MockMvc mockMvc;
    private User sender;
    private User receiver;
    private User common;

    @BeforeEach
    void setUp() {
        sender = userRepository.saveAndFlush(User.createNew("신청자", null, "flow-sender@example.com"));
        receiver = userRepository.saveAndFlush(User.createNew("수신자", null, "flow-receiver@example.com"));
        common = userRepository.saveAndFlush(User.createNew("공통 친구", null, "flow-common@example.com"));
        UserService authentication = mock(UserService.class);
        for (User user : new User[]{sender, receiver, common}) {
            when(authentication.getMe(token(user))).thenReturn(
                    new MyProfileResponse(user.getId(), user.getDisplayName(), null, true)
            );
        }
        mockMvc = MockMvcBuilders.standaloneSetup(new FriendController(friendService))
                .setCustomArgumentResolvers(new CurrentUserResolver(authentication))
                .setControllerAdvice(GlobalExceptionHandlerTestFixture.globalExceptionHandler())
                .build();
        acceptFixture(sender, common);
        acceptFixture(common, receiver);
    }

    @Test
    @DisplayName("실제 서비스와 DB로 초대·검색·요청·거절·재요청·수락·친구 끊기 흐름 및 행위자 권한을 검증한다")
    void friendshipFlow_preservesRecipientApprovalAndBothSidesOfRelationship() throws Exception {
        JsonNode invite = json(perform(receiver, get("/friends/invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(receiver.getEmail())));
        String code = invite.path("code").asText();
        perform(receiver, get("/friends/invite"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code));
        perform(sender, get("/friends/invite/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(receiver.getId()))
                .andExpect(jsonPath("$.relationshipStatus").value("NONE"))
                .andExpect(jsonPath("$.daysSinceStart").isNumber())
                .andExpect(jsonPath("$.targetSuccessCount").value(0))
                .andExpect(jsonPath("$.email").doesNotExist());
        perform(sender, get("/friends/search").param("email", receiver.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(receiver.getId()))
                .andExpect(jsonPath("$.mutualFriendCount").value(1))
                .andExpect(jsonPath("$.mutualFriendPreviewName").value(common.getDisplayName()))
                .andExpect(jsonPath("$.email").doesNotExist());
        perform(sender, get("/friends/search").param("email", "flow-receive@example.com"))
                .andExpect(status().isNotFound());

        long original = sendRequest(sender, receiver);
        perform(sender, get("/friends/requests/sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(original))
                .andExpect(jsonPath("$[0].user.userId").value(receiver.getId()));
        perform(receiver, get("/friends/requests/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(original))
                .andExpect(jsonPath("$[0].user.email").value(sender.getEmail()));
        perform(sender, get("/friends/invite/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationshipStatus").value("PENDING_SENT"));
        perform(receiver, post("/friends/requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetUserId\":" + sender.getId() + "}"))
                .andExpect(status().isConflict());
        perform(sender, delete("/friends/requests/{id}", original)).andExpect(status().isForbidden());
        perform(common, delete("/friends/requests/{id}", original)).andExpect(status().isForbidden());
        perform(sender, post("/friends/requests/{id}/accept", original)).andExpect(status().isForbidden());
        perform(common, post("/friends/requests/{id}/accept", original)).andExpect(status().isForbidden());
        assertThat(friendRepository.findById(original)).get().matches(friend -> friend.isPending());

        perform(receiver, delete("/friends/requests/{id}", original)).andExpect(status().isNoContent());
        assertThat(friendRepository.findById(original)).isEmpty();
        long replacement = sendRequest(sender, receiver);
        assertThat(replacement).isNotEqualTo(original);
        perform(receiver, delete("/friends/requests/{id}", original)).andExpect(status().isNotFound());
        assertThat(friendRepository.findById(replacement)).get().matches(friend -> friend.isPending());
        JsonNode accepted = json(perform(receiver, post("/friends/requests/{id}/accept", replacement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(sender.getEmail())));
        long friendshipId = accepted.path("friendshipId").asLong();
        assertThat(friendshipId).isEqualTo(replacement);
        assertFriendListed(sender, receiver, true);
        assertFriendListed(receiver, sender, true);
        perform(sender, get("/friends/requests/sent")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        perform(receiver, get("/friends/requests/received")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        perform(common, delete("/friends/{id}", friendshipId)).andExpect(status().isForbidden());
        assertThat(friendRepository.findById(friendshipId)).get().matches(friend -> friend.isAccepted());

        perform(sender, delete("/friends/{id}", friendshipId)).andExpect(status().isNoContent());
        assertFriendListed(sender, receiver, false);
        assertFriendListed(receiver, sender, false);
        long requestedAgain = sendRequest(sender, receiver);
        assertThat(requestedAgain).isNotEqualTo(friendshipId);
        perform(receiver, post("/friends/requests/{id}/accept", requestedAgain)).andExpect(status().isOk());
        perform(receiver, delete("/friends/{id}", requestedAgain)).andExpect(status().isNoContent());
        assertFriendListed(sender, receiver, false);
        assertFriendListed(receiver, sender, false);
    }

    private void acceptFixture(User from, User to) {
        long id = friendService.sendRequest(from.getId(), to.getId()).requestId();
        friendService.acceptRequest(id, to.getId());
    }

    private long sendRequest(User from, User to) throws Exception {
        return json(perform(from, post("/friends/requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetUserId\":" + to.getId() + "}"))
                .andExpect(status().isCreated())).path("requestId").asLong();
    }

    private void assertFriendListed(User viewer, User target, boolean present) throws Exception {
        perform(viewer, get("/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].user.userId", present
                        ? hasItem(target.getId().intValue()) : not(hasItem(target.getId().intValue()))));
    }

    private ResultActions perform(User actor, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(actor)));
    }

    private String token(User user) {
        return "flow-user-" + user.getId();
    }

    private JsonNode json(ResultActions result) throws Exception {
        return JsonMapper.builder().findAndAddModules().build()
                .readTree(result.andReturn().getResponse().getContentAsString());
    }
}
