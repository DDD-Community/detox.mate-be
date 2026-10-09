package com.detoxmate.friend;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.common.error.GlobalExceptionHandlerTestFixture;
import com.detoxmate.friend.controller.FriendController;
import com.detoxmate.friend.service.FriendService;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.domain.UserCode;
import com.detoxmate.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FriendUserCodeSearchHttpIntegrationTest {

    private static final String VIEWER_CODE = "ABCDE";
    private static final String TARGET_CODE = "MNPQR";

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private User viewer;
    private User target;

    @BeforeEach
    void setUp() {
        viewer = userRepository.saveAndFlush(User.createNew(
                "검색자", null, "code-search-viewer@example.com", new UserCode(VIEWER_CODE)));
        target = userRepository.saveAndFlush(User.createNew(
                "찾을 친구", "profile-images/friend/profile.png", "code-search-target@example.com",
                new UserCode(TARGET_CODE)));
        mockMvc = MockMvcBuilders.standaloneSetup(new FriendController(friendService))
                .setCustomArgumentResolvers(currentUserResolver)
                .setControllerAdvice(GlobalExceptionHandlerTestFixture.globalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("사용자 코드로 검색하면 일치하는 사용자의 공개 정보와 관계를 반환한다")
    void searchByUserCode_returnsMatchingUserWithoutExposingEmail() throws Exception {
        // when & then
        authenticated(get("/friends/search").param("userCode", TARGET_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(target.getId()))
                .andExpect(jsonPath("$.displayName").value("찾을 친구"))
                .andExpect(jsonPath("$.profileImageUrl")
                        .value("https://example.com/media/profile-images/friend/profile.png"))
                .andExpect(jsonPath("$.relationshipStatus").value("NONE"))
                .andExpect(jsonPath("$.requestId").isEmpty())
                .andExpect(jsonPath("$.mutualFriendCount").value(0))
                .andExpect(jsonPath("$.mutualFriendPreviewName").isEmpty())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("이메일이 없는 사용자도 사용자 코드로 검색할 수 있다")
    void searchByUserCode_returnsUserWithoutEmail() throws Exception {
        // given
        User withoutEmail = userRepository.saveAndFlush(User.createNew(
                "이메일 없는 친구", new UserCode("23456")));

        // when & then
        authenticated(get("/friends/search").param("userCode", withoutEmail.getUserCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(withoutEmail.getId()))
                .andExpect(jsonPath("$.displayName").value("이메일 없는 친구"))
                .andExpect(jsonPath("$.relationshipStatus").value("NONE"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"mnpqr", "MnPqR", "  MNPQR  ", " \tmnpqr\n "})
    @DisplayName("5자리 코드의 대소문자와 앞뒤 공백을 정규화해 동일 사용자를 찾는다")
    void searchByUserCode_normalizesSupportedInputFormats(String input) throws Exception {
        // when & then
        authenticated(get("/friends/search").param("userCode", input))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(target.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "MNPQ", "MNPQRS", "MNPQ0", "MNPQO", "MNPQ1", "MNPQI",
            "MN-PQR", "MN--PQR", "MN PQR", "MNPQRSTUV2", "MNPQR-STUV2",
            "code-search-target@example.com"
    })
    @DisplayName("빈 값과 잘못된 길이·문자·하이픈 및 이메일 검색키는 400으로 거부한다")
    void searchByUserCode_rejectsInvalidInput(String input) throws Exception {
        // when & then
        authenticated(get("/friends/search").param("userCode", input))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("userCode 검색키가 누락되면 400을 반환한다")
    void searchByUserCode_rejectsMissingCode() throws Exception {
        // when & then
        authenticated(get("/friends/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("일치하는 이메일만 전달해도 사용자 코드 없이 검색할 수 없다")
    void searchByUserCode_rejectsEmailOnlySearch() throws Exception {
        // when & then
        authenticated(get("/friends/search").param("email", target.getEmail()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("형식이 올바르지만 한 글자가 다른 코드는 비슷한 사용자를 반환하지 않는다")
    void searchByUserCode_returnsNotFoundForNonMatchingCode() throws Exception {
        // when & then
        authenticated(get("/friends/search").param("userCode", "MNPQS"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("탈퇴한 사용자는 보존된 사용자 코드로 검색해도 찾을 수 없다")
    void searchByUserCode_excludesWithdrawnTarget() throws Exception {
        // given
        target.withdraw();
        userRepository.saveAndFlush(target);
        entityManager.clear();

        // when & then
        authenticated(get("/friends/search").param("userCode", TARGET_CODE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("상태가 NULL인 기존 계정도 사용자 코드가 있으면 검색할 수 있다")
    void searchByUserCode_includesLegacyTargetWithNullStatus() throws Exception {
        // given
        jdbcTemplate.update("update users set status = null where user_id = ?", target.getId());
        entityManager.clear();

        // when & then
        authenticated(get("/friends/search").param("userCode", TARGET_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(target.getId()))
                .andExpect(jsonPath("$.relationshipStatus").value("NONE"));
    }

    @Test
    @DisplayName("코드가 없는 기존 계정은 검색 결과에 나타나지 않으며 코드도 발급되지 않는다")
    void searchByUserCode_doesNotAssignCodeToLegacyTarget() throws Exception {
        // given
        jdbcTemplate.update("update users set user_code = null where user_id = ?", target.getId());
        entityManager.clear();

        // when
        authenticated(get("/friends/search").param("userCode", TARGET_CODE))
                .andExpect(status().isNotFound());

        // then
        entityManager.clear();
        assertThat(userRepository.findById(target.getId()).orElseThrow().getUserCode()).isNull();
    }

    @Test
    @DisplayName("인증 없이 사용자 코드로 검색하면 401을 반환한다")
    void searchByUserCode_requiresAuthentication() throws Exception {
        // when & then
        mockMvc.perform(get("/friends/search").param("userCode", TARGET_CODE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("탈퇴한 사용자의 토큰으로 검색하면 401을 반환한다")
    void searchByUserCode_rejectsWithdrawnViewer() throws Exception {
        // given
        viewer.withdraw();
        userRepository.saveAndFlush(viewer);
        entityManager.clear();

        // when & then
        authenticated(get("/friends/search").param("userCode", TARGET_CODE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private ResultActions authenticated(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtTokenProvider.createAccessToken(viewer.getId())));
    }
}
