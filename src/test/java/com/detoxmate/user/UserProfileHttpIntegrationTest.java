package com.detoxmate.user;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.common.error.GlobalExceptionHandlerTestFixture;
import com.detoxmate.support.UserFixtures;
import com.detoxmate.user.controller.UserController;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserProfileHttpIntegrationTest {

    private static final String IMAGE_BASE_URL = "https://example.com/media/";

    @DynamicPropertySource
    static void useIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:user-profile-three-fields;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    }

    @Autowired
    private UserController userController;
    @Autowired
    private CurrentUserResolver currentUserResolver;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_LONG_FOR_INTS);
    private MockMvc mockMvc;
    private User viewer;

    @BeforeEach
    void setUp() {
        // Fixture writes commit independently of the HTTP request under test.
        userRepository.deleteAllInBatch();
        viewer = userRepository.saveAndFlush(UserFixtures.createUser("이전이름"));
        viewer.updatePushNotificationEnabled(false);
        viewer.changeProfileImageObjectKey("profile-images/" + viewer.getId() + "/before.png");
        viewer = userRepository.saveAndFlush(viewer);

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(currentUserResolver)
                .setControllerAdvice(GlobalExceptionHandlerTestFixture.globalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("내 프로필 조회는 사용자 ID와 알림 OFF를 포함한 다섯 필드를 반환한다")
    void getMe_returnsProfileWithIdentityAndDisabledNotifications() throws Exception {
        // when
        JsonNode response = responseBody(authenticated(get("/users/me"))
                .andExpect(status().isOk()));

        // then
        assertThat(response).isEqualTo(objectMapper.createObjectNode()
                .put("id", viewer.getId())
                .put("pushNotificationEnabled", false)
                .put("displayName", viewer.getDisplayName())
                .put("userCode", viewer.getUserCode())
                .put("profileImageUrl", IMAGE_BASE_URL + viewer.getProfileImageObjectKey()));
    }

    @Test
    @DisplayName("이미지가 없는 계정도 다섯 필드와 명시적인 이미지 null을 반환한다")
    void getMe_preservesExplicitNullImageField() throws Exception {
        // given
        viewer.changeProfileImageObjectKey(null);
        userRepository.saveAndFlush(viewer);

        // when
        JsonNode response = responseBody(authenticated(get("/users/me"))
                .andExpect(status().isOk()));

        // then
        assertThat(response).isEqualTo(objectMapper.createObjectNode()
                .put("id", viewer.getId())
                .put("pushNotificationEnabled", false)
                .put("displayName", viewer.getDisplayName())
                .put("userCode", viewer.getUserCode())
                .putNull("profileImageUrl"));
        assertThat(reloadViewer().getUserCode()).isEqualTo(viewer.getUserCode());
    }

    @Test
    @DisplayName("프로필 수정 응답도 사용자 ID와 저장된 알림 OFF를 반환한다")
    void updateMe_returnsUpdatedProfileWithIdentityAndDisabledNotifications() throws Exception {
        // given
        String imageKey = "profile-images/" + viewer.getId() + "/updated.png";
        String body = objectMapper.createObjectNode()
                .put("displayName", "새이름")
                .put("profileImageObjectKey", imageKey)
                .toString();

        // when
        JsonNode response = responseBody(patchProfile(body).andExpect(status().isOk()));

        // then
        assertThat(response).isEqualTo(objectMapper.createObjectNode()
                .put("id", viewer.getId())
                .put("pushNotificationEnabled", false)
                .put("displayName", "새이름")
                .put("userCode", viewer.getUserCode())
                .put("profileImageUrl", IMAGE_BASE_URL + imageKey));
    }

    @Test
    @DisplayName("한 요청의 닉네임과 이미지 변경을 함께 저장하고 사용자 코드를 보존한다")
    void updateMe_commitsDisplayNameAndImageTogether() throws Exception {
        // given
        String imageKey = "profile-images/" + viewer.getId() + "/updated.png";
        String body = objectMapper.createObjectNode()
                .put("displayName", "새이름")
                .put("profileImageObjectKey", imageKey)
                .toString();

        // when
        patchProfile(body).andExpect(status().isOk());

        // then
        User persisted = reloadViewer();
        assertThat(persisted.getDisplayName()).isEqualTo("새이름");
        assertThat(persisted.getProfileImageObjectKey()).isEqualTo(imageKey);
        assertThat(persisted.getUserCode()).isEqualTo(viewer.getUserCode());
    }

    @Test
    @DisplayName("닉네임과 다른 사용자의 이미지 경로를 함께 보내면 두 저장값을 모두 유지한다")
    void updateMe_rollsBackBothFieldsWhenImageBelongsToAnotherUser() throws Exception {
        // given
        User otherUser = userRepository.saveAndFlush(UserFixtures.createUser("다른사용자"));
        String body = objectMapper.createObjectNode()
                .put("displayName", "변경시도")
                .put("profileImageObjectKey", "profile-images/" + otherUser.getId() + "/updated.png")
                .toString();

        // when
        patchProfile(body).andExpect(status().isBadRequest());

        // then
        User persisted = reloadViewer();
        assertThat(persisted.getDisplayName()).isEqualTo(viewer.getDisplayName());
        assertThat(persisted.getProfileImageObjectKey()).isEqualTo(viewer.getProfileImageObjectKey());
        assertThat(persisted.getUserCode()).isEqualTo(viewer.getUserCode());
    }

    @Test
    @DisplayName("닉네임만 수정하면 저장된 프로필 이미지를 유지한다")
    void updateMe_preservesImageWhenFieldIsOmitted() throws Exception {
        // when
        patchProfile("""
                { "displayName": "이름만변경" }
                """).andExpect(status().isOk());

        // then
        User persisted = reloadViewer();
        assertThat(persisted.getDisplayName()).isEqualTo("이름만변경");
        assertThat(persisted.getProfileImageObjectKey()).isEqualTo(viewer.getProfileImageObjectKey());
    }

    @Test
    @DisplayName("닉네임 변경과 명시적 이미지 null을 함께 보내면 이름을 저장하고 이미지를 제거한다")
    void updateMe_commitsDisplayNameAndExplicitImageRemovalTogether() throws Exception {
        // when
        JsonNode response = responseBody(patchProfile("""
                { "displayName": "이미지삭제", "profileImageObjectKey": null }
                """).andExpect(status().isOk()));

        // then
        User persisted = reloadViewer();
        assertThat(persisted.getDisplayName()).isEqualTo("이미지삭제");
        assertThat(persisted.getProfileImageObjectKey()).isNull();
        assertThat(response.has("profileImageUrl")).isTrue();
        assertThat(response.get("profileImageUrl").isNull()).isTrue();
    }

    private ResultActions patchProfile(String body) throws Exception {
        return authenticated(patch("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions authenticated(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtTokenProvider.createAccessToken(viewer.getId())));
    }

    private JsonNode responseBody(ResultActions response) throws Exception {
        return objectMapper.readTree(response.andReturn().getResponse().getContentAsString());
    }

    private User reloadViewer() {
        return userRepository.findById(viewer.getId()).orElseThrow();
    }
}
