package com.detoxmate.friend;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.domain.FriendInvite;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.repository.FriendInviteRepository;
import com.detoxmate.friend.repository.FriendRepository;
import com.detoxmate.support.UserFixtures;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(RestDocumentationExtension.class)
class FriendProfileHttpIntegrationTest {

    private static final String IMAGE_BASE_URL = "https://example.com/media/";
    private static final LocalDateTime ACCEPTED_AT = LocalDateTime.of(2026, 10, 9, 12, 0);
    private static final String PROFILE_DESCRIPTION = "로그인 사용자가 직접 수락한 활성 친구의 프로필을 조회한다. "
            + "성공 응답의 relationshipStatus는 항상 FRIEND이다. "
            + "GET /friends의 user.userId를 경로에 전달한다. userCode는 GET /users/me와 같은 사용자 코드이며 기존 계정은 null일 수 있다. "
            + "friends에는 대상의 수락된 활성 친구만 수락시각 내림차순, 같은 시각이면 관계 ID 내림차순으로 표시하며 조회자도 포함한다. "
            + "각 항목은 이름·사진만 제공하므로 화면에서 클릭 이동을 연결하지 않는다. 서버는 직접 친구 관계를 검사하며 탐색 경로를 추적하지 않는다. "
            + "본인·대기 관계·비친구는 403, 미존재·탈퇴한 대상은 404, 인증 누락·실패·탈퇴한 조회자는 401이다. "
            + "그룹 가입과 무관하며 조회만으로 사용자 코드나 초대코드를 생성하지 않는다.";

    @DynamicPropertySource
    static void useIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:friend-profile-http;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    }

    @Autowired
    private WebApplicationContext applicationContext;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FriendRepository friendRepository;
    @Autowired
    private FriendInviteRepository friendInviteRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_LONG_FOR_INTS);
    private MockMvc mockMvc;
    private User viewer;
    private User target;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        viewer = userRepository.saveAndFlush(UserFixtures.createUser("조회자"));
        target = userRepository.saveAndFlush(UserFixtures.createUser(
                "친구", "profile-images/friend/profile.png", "friend-profile@example.com"));
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    @DisplayName("수락된 친구의 프로필과 표시 전용 친구 목록을 반환한다")
    void getProfile_returnsAcceptedFriendProfile() throws Exception {
        // given
        acceptFriendship(viewer, target, ACCEPTED_AT);

        // when
        JsonNode response = responseBody(authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isOk())
                .andDo(documentProfile("friends/profile-get", "FriendProfileResponse", profileFields(), true)));

        // then
        assertThat(response).isEqualTo(objectMapper.createObjectNode()
                .put("userId", target.getId())
                .put("displayName", target.getDisplayName())
                .put("profileImageUrl", IMAGE_BASE_URL + target.getProfileImageObjectKey())
                .put("userCode", target.getUserCode())
                .put("relationshipStatus", "FRIEND")
                .set("friends", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("displayName", viewer.getDisplayName())
                                .putNull("profileImageUrl"))));
    }

    @Test
    @DisplayName("상대방이 먼저 요청한 수락 관계도 친구 프로필을 조회할 수 있다")
    void getProfile_allowsAcceptedFriendshipInReverseDirection() throws Exception {
        // given
        acceptFriendship(target, viewer, ACCEPTED_AT);

        // when & then
        authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(target.getId()))
                .andExpect(jsonPath("$.relationshipStatus").value("FRIEND"));
    }

    @Test
    @DisplayName("친구의 활성 수락 친구를 최신순으로 표시하고 공통 친구에도 탐색 정보를 노출하지 않는다")
    void getProfile_listsOnlyAcceptedActiveFriendsInStableOrderWithoutNavigationFields() throws Exception {
        // given
        acceptFriendship(viewer, target, ACCEPTED_AT.minusDays(1));
        User mutual = userRepository.saveAndFlush(UserFixtures.createUser("공통 친구", "profile-images/mutual.png"));
        User legacy = userRepository.saveAndFlush(UserFixtures.createUser("기존 친구"));
        User newest = userRepository.saveAndFlush(UserFixtures.createUser("최신 친구", "profile-images/newest.png"));
        User withdrawn = userRepository.saveAndFlush(UserFixtures.createUser("탈퇴 친구"));
        User outgoingPending = userRepository.saveAndFlush(UserFixtures.createUser("보낸 요청"));
        User incomingPending = userRepository.saveAndFlush(UserFixtures.createUser("받은 요청"));
        User viewerOnly = userRepository.saveAndFlush(UserFixtures.createUser("조회자에게만 친구"));
        acceptFriendship(target, mutual, ACCEPTED_AT);
        acceptFriendship(legacy, target, ACCEPTED_AT.plusHours(1));
        acceptFriendship(target, newest, ACCEPTED_AT.plusHours(1));
        acceptFriendship(target, withdrawn, ACCEPTED_AT.plusDays(1));
        acceptFriendship(viewer, mutual, ACCEPTED_AT);
        acceptFriendship(viewer, viewerOnly, ACCEPTED_AT);
        friendRepository.saveAndFlush(Friend.request(target.getId(), outgoingPending.getId()));
        friendRepository.saveAndFlush(Friend.request(incomingPending.getId(), target.getId()));
        withdrawn.withdraw();
        userRepository.saveAndFlush(withdrawn);
        jdbcTemplate.update("update users set status = null where user_id = ?", legacy.getId());
        entityManager.clear();

        // when
        JsonNode response = responseBody(authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isOk()));

        // then
        assertThat(response.get("friends")).isEqualTo(objectMapper.createArrayNode()
                .add(displayOnlyFriend(newest))
                .add(displayOnlyFriend(legacy))
                .add(displayOnlyFriend(mutual))
                .add(displayOnlyFriend(viewer)));
    }

    @Test
    @DisplayName("직접 친구가 아닌 친구의 친구는 프로필을 조회할 수 없다")
    void getProfile_forbidsFriendOfFriendWithoutDirectFriendship() throws Exception {
        // given
        User intermediate = userRepository.saveAndFlush(UserFixtures.createUser("연결 친구"));
        acceptFriendship(viewer, intermediate, ACCEPTED_AT);
        acceptFriendship(intermediate, target, ACCEPTED_AT);

        // when & then
        authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403))
                .andDo(documentProfile("friends/profile-forbidden", "ErrorResponse", errorFields(), true));
    }

    @Test
    @DisplayName("자기 자신은 친구 프로필 API로 조회할 수 없다")
    void getProfile_forbidsSelf() throws Exception {
        // when & then
        authenticated(get("/friends/{friendUserId}/profile", viewer.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("어느 방향이든 대기 중인 요청만으로 친구 프로필을 조회할 수 없다")
    void getProfile_forbidsPendingFriendshipInEitherDirection(boolean reverse) throws Exception {
        // given
        friendRepository.saveAndFlush(reverse
                ? Friend.request(target.getId(), viewer.getId())
                : Friend.request(viewer.getId(), target.getId()));

        // when & then
        authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("존재하지 않는 친구 프로필은 404를 반환한다")
    void getProfile_returnsNotFoundForMissingTarget() throws Exception {
        // when & then
        authenticated(get("/friends/{friendUserId}/profile", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andDo(documentProfile("friends/profile-not-found", "ErrorResponse", errorFields(), true));
    }

    @Test
    @DisplayName("기존 수락 관계가 남아 있어도 탈퇴한 친구 프로필은 404를 반환한다")
    void getProfile_returnsNotFoundForWithdrawnTarget() throws Exception {
        // given
        acceptFriendship(viewer, target, ACCEPTED_AT);
        target.withdraw();
        userRepository.saveAndFlush(target);
        entityManager.clear();

        // when & then
        authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("인증 없이 친구 프로필을 조회하면 401을 반환한다")
    void getProfile_requiresAuthentication() throws Exception {
        // given
        acceptFriendship(viewer, target, ACCEPTED_AT);

        // when & then
        mockMvc.perform(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andDo(documentProfile("friends/profile-unauthorized", "ErrorResponse", errorFields(), false));
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 친구 프로필을 조회하면 401을 반환한다")
    void getProfile_rejectsInvalidToken() throws Exception {
        // given
        acceptFriendship(viewer, target, ACCEPTED_AT);

        // when & then
        mockMvc.perform(get("/friends/{friendUserId}/profile", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("탈퇴한 조회자의 토큰으로 친구 프로필을 조회하면 401을 반환한다")
    void getProfile_rejectsWithdrawnViewer() throws Exception {
        // given
        acceptFriendship(viewer, target, ACCEPTED_AT);
        viewer.withdraw();
        userRepository.saveAndFlush(viewer);
        entityManager.clear();

        // when & then
        authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("토큰이 가리키는 조회자가 없으면 친구 프로필 조회에 401을 반환한다")
    void getProfile_rejectsMissingViewer() throws Exception {
        // when & then
        mockMvc.perform(get("/friends/{friendUserId}/profile", target.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtTokenProvider.createAccessToken(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("상태와 코드가 없는 기존 친구의 프로필을 조회해도 코드나 초대코드를 생성하지 않는다")
    void getProfile_preservesLegacyNullCodeAndImageWithoutCreatingInvite() throws Exception {
        // given
        acceptFriendship(viewer, target, ACCEPTED_AT);
        jdbcTemplate.update("update users set status = null, user_code = null, profile_image_object_key = null where user_id = ?",
                target.getId());
        entityManager.clear();
        long inviteCountBefore = friendInviteRepository.count();

        // when
        JsonNode first = responseBody(authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isOk()));
        JsonNode repeated = responseBody(authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isOk()));

        // then
        assertThat(first).isEqualTo(objectMapper.createObjectNode()
                .put("userId", target.getId())
                .put("displayName", target.getDisplayName())
                .putNull("profileImageUrl")
                .putNull("userCode")
                .put("relationshipStatus", "FRIEND")
                .set("friends", objectMapper.createArrayNode().add(displayOnlyFriend(viewer))));
        assertThat(repeated).isEqualTo(first);
        entityManager.flush();
        entityManager.clear();
        assertThat(userRepository.findById(target.getId()).orElseThrow().getUserCode()).isNull();
        assertThat(friendInviteRepository.count()).isEqualTo(inviteCountBefore);
        assertThat(friendInviteRepository.findByUserId(target.getId())).isEmpty();
    }

    @Test
    @DisplayName("친구 프로필에는 레거시 초대코드 대신 내 프로필과 같은 사용자 코드를 반환한다")
    void getProfile_returnsUserCodeAndPreservesExistingInvite() throws Exception {
        // given
        acceptFriendship(viewer, target, ACCEPTED_AT);
        FriendInvite invite = friendInviteRepository.saveAndFlush(FriendInvite.create(target.getId(), "legacy-invite-code"));
        entityManager.clear();
        JsonNode ownProfile = responseBody(mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtTokenProvider.createAccessToken(target.getId())))
                .andExpect(status().isOk()));

        // when
        JsonNode friendProfile = responseBody(authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isOk()));

        // then
        assertThat(friendProfile.get("userCode")).isEqualTo(ownProfile.get("userCode"));
        assertThat(friendProfile.get("userCode").asText()).isEqualTo(target.getUserCode());
        entityManager.flush();
        entityManager.clear();
        FriendInvite unchanged = friendInviteRepository.findByUserId(target.getId()).orElseThrow();
        assertThat(unchanged.getId()).isEqualTo(invite.getId());
        assertThat(unchanged.getCode()).isEqualTo("legacy-invite-code");
    }

    @Test
    @DisplayName("친구 관계를 해제하면 이전에 조회했던 친구 프로필에도 접근할 수 없다")
    void getProfile_revokesAccessAfterUnfriend() throws Exception {
        // given
        long friendshipId = acceptFriendship(viewer, target, ACCEPTED_AT);
        authenticated(get("/friends/{friendUserId}/profile", target.getId())).andExpect(status().isOk());

        // when
        authenticated(delete("/friends/{friendshipId}", friendshipId)).andExpect(status().isNoContent());

        // then
        authenticated(get("/friends/{friendUserId}/profile", target.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        assertThat(friendRepository.findById(friendshipId)).isEmpty();
    }

    private JsonNode displayOnlyFriend(User friend) {
        return objectMapper.createObjectNode()
                .put("displayName", friend.getDisplayName())
                .put("profileImageUrl", friend.getProfileImageObjectKey() == null
                        ? null : IMAGE_BASE_URL + friend.getProfileImageObjectKey());
    }

    private ResultHandler documentProfile(String identifier, String schemaName, FieldDescriptor[] fields,
                                          boolean authenticated) {
        HeaderDescriptor[] headers = authenticated
                ? new HeaderDescriptor[]{headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")}
                : new HeaderDescriptor[0];
        return document(identifier,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headers),
                pathParameters(parameterWithName("friendUserId").description("GET /friends의 user.userId: 친구 사용자 ID")),
                responseFields(fields),
                resource(ResourceSnippetParameters.builder()
                        .tag("Friend")
                        .summary("Get friend profile")
                        .description(PROFILE_DESCRIPTION)
                        .requestHeaders(headers)
                        .pathParameters(com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("friendUserId")
                                .type(SimpleType.INTEGER).description("친구 사용자 ID"))
                        .responseSchema(schema(schemaName))
                        .responseFields(fields)
                        .build()));
    }

    private FieldDescriptor[] profileFields() {
        return new FieldDescriptor[]{
                fieldWithPath("userId").type(JsonFieldType.NUMBER).description("조회 대상 친구 사용자 ID"),
                fieldWithPath("displayName").type(JsonFieldType.STRING).description("친구 공개 닉네임"),
                fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).optional().description("친구 프로필 이미지 읽기 URL, 없으면 null"),
                fieldWithPath("userCode").type(JsonFieldType.STRING).optional().description("내 프로필과 같은 사용자 코드, 기존 계정은 null 가능"),
                new EnumFields(FriendRelationshipStatus.class).withPath("relationshipStatus")
                        .description("현재 사용자와의 관계 상태 (NONE | SELF | PENDING_SENT | PENDING_RECEIVED | FRIEND)"),
                fieldWithPath("friends").type(JsonFieldType.ARRAY).description("대상의 수락된 활성 친구 목록. 조회자 포함, acceptedAt DESC, id DESC"),
                fieldWithPath("friends[].displayName").type(JsonFieldType.STRING).description("표시 전용 친구 닉네임. 항목에 클릭 이동을 연결하지 않음"),
                fieldWithPath("friends[].profileImageUrl").type(JsonFieldType.STRING).optional().description("표시 전용 친구 프로필 이미지 URL, 없으면 null")
        };
    }

    private FieldDescriptor[] errorFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드")
        };
    }

    private long acceptFriendship(User from, User to, LocalDateTime acceptedAt) {
        Friend request = friendRepository.saveAndFlush(Friend.request(from.getId(), to.getId()));
        assertThat(friendRepository.acceptPendingRequest(request.getId(), to.getId(), acceptedAt)).isEqualTo(1);
        return request.getId();
    }

    private ResultActions authenticated(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtTokenProvider.createAccessToken(viewer.getId())));
    }

    private JsonNode responseBody(ResultActions response) throws Exception {
        return objectMapper.readTree(response.andReturn().getResponse().getContentAsString());
    }
}
