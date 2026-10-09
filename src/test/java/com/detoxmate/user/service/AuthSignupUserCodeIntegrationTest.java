package com.detoxmate.user.service;

import com.detoxmate.auth.RefreshTokenProvider;
import com.detoxmate.auth.dto.AppleSocialLoginRequest;
import com.detoxmate.auth.dto.AuthLoginResponse;
import com.detoxmate.auth.repository.RefreshTokenSessionRepository;
import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.domain.UserCode;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.provider-token.encryption.key=AAAAAAAAAAAAAAAAAAAAAA==")
@ActiveProfiles("test")
@Import(AuthSignupUserCodeIntegrationTest.SignupTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthSignupUserCodeIntegrationTest {

    private static final String EXISTING_CODE = "ABCDE";
    private static final String NEW_CODE = "FGHJK";
    private static final String APPLE_ID_TOKEN = "apple-id-token";
    private static final String APPLE_NONCE = "apple-raw-nonce";
    private static final String APPLE_AUTHORIZATION_CODE = "single-use-apple-authorization-code";

    @DynamicPropertySource
    static void useIsolatedDatabase(DynamicPropertyRegistry registry) {
        String mysqlUrl = System.getenv("USER_CODE_MYSQL_URL");
        if (mysqlUrl == null || mysqlUrl.isBlank()) {
            registry.add("spring.datasource.url", () ->
                    "jdbc:h2:mem:auth-signup-user-code;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            return;
        }
        registry.add("spring.datasource.url", () -> mysqlUrl);
        registry.add("spring.datasource.username", () -> System.getenv("USER_CODE_MYSQL_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("USER_CODE_MYSQL_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private DevAuthService devAuthService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SocialLoginUserRepository socialLoginUserRepository;
    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;
    @Autowired
    private ProviderTokenCipher providerTokenCipher;
    @Autowired
    private ScriptedUserCodeGenerator codeGenerator;
    @Autowired
    private ScriptedRefreshTokenProvider refreshTokenProvider;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private KakaoRestApiClient kakaoRestApiClient;
    @MockitoBean
    private AppleIdentityTokenVerifier appleIdentityTokenVerifier;
    @MockitoBean
    private AppleRestApiClient appleRestApiClient;

    @BeforeEach
    void resetDatabaseAndRandomness() {
        refreshTokenSessionRepository.deleteAllInBatch();
        socialLoginUserRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        codeGenerator.use();
        refreshTokenProvider.reset();
    }

    @Test
    @DisplayName("새 테스트 계정으로 가입하면 저장된 사용자에게 5자리 개인 코드가 있다")
    void testLogin_persistsCanonicalUserCodeForNewUser() {
        // given
        codeGenerator.use(NEW_CODE);

        // when
        AuthLoginResponse response = devAuthService.testLogin("front-a");
        User persistedUser = userRepository.findById(response.id()).orElseThrow();

        // then
        assertThat(response.isNewUser()).isTrue();
        assertThat(persistedUser.getUserCode()).isEqualTo(NEW_CODE);
        assertThat(persistedUser.getEmail()).isNull();
        assertThat(rowCounts()).isEqualTo(new RowCounts(1, 1, 1));
    }

    @ParameterizedTest
    @EnumSource(value = SocialProvider.class, names = {"KAKAO", "APPLE"})
    @DisplayName("이메일 없는 소셜 계정도 개인 코드와 함께 가입한다")
    void socialLogin_persistsCodeWhenEmailIsAbsent(SocialProvider provider) {
        // given
        codeGenerator.use(NEW_CODE);

        // when
        AuthLoginResponse response = loginWithoutEmail(provider);
        User persistedUser = userRepository.findById(response.id()).orElseThrow();

        // then
        assertThat(response.isNewUser()).isTrue();
        assertThat(persistedUser.getUserCode()).isEqualTo(NEW_CODE);
        assertThat(persistedUser.getEmail()).isNull();
        assertThat(rowCounts()).isEqualTo(new RowCounts(1, 1, 1));
        assertThat(socialLoginUserRepository.findAll().getFirst().getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("재로그인하면 기존 코드와 수정한 이름 및 프로필과 이메일을 보존한다")
    void loginWithKakao_preservesExistingIdentityAndChangedProfile() {
        // given
        User existing = persistUser("기존 이름", "profile-images/existing.png", "stored@example.com", EXISTING_CODE);
        existing.changeDisplayName("수정한 이름");
        userRepository.saveAndFlush(existing);
        socialLoginUserRepository.saveAndFlush(SocialLoginUser.link(existing, SocialProvider.KAKAO, "existing-kakao"));
        when(kakaoRestApiClient.getUserInfo("kakao-token"))
                .thenReturn(new KakaoUserInfo("existing-kakao", "소셜 새 이름", "different@example.com"));

        // when
        AuthLoginResponse response = authService.loginWithKakao("kakao-token");
        User persistedUser = userRepository.findById(existing.getId()).orElseThrow();

        // then
        assertThat(response.isNewUser()).isFalse();
        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.displayName()).isEqualTo("수정한 이름");
        assertThat(persistedUser.getUserCode()).isEqualTo(EXISTING_CODE);
        assertThat(persistedUser.getDisplayName()).isEqualTo("수정한 이름");
        assertThat(persistedUser.getProfileImageObjectKey()).isEqualTo("profile-images/existing.png");
        assertThat(persistedUser.getEmail()).isEqualTo("stored@example.com");
        assertThat(rowCounts()).isEqualTo(new RowCounts(1, 1, 1));
    }

    @Test
    @DisplayName("기존 계정의 비어 있는 이메일을 등록해도 개인 코드는 유지한다")
    void loginWithKakao_registersMissingEmailWithoutReplacingCode() {
        // given
        User existing = persistUser("기존 사용자", null, null, EXISTING_CODE);
        socialLoginUserRepository.saveAndFlush(SocialLoginUser.link(existing, SocialProvider.KAKAO, "existing-kakao"));
        when(kakaoRestApiClient.getUserInfo("kakao-token"))
                .thenReturn(new KakaoUserInfo("existing-kakao", "새 이름", " New@Example.com "));

        // when
        authService.loginWithKakao("kakao-token");
        User persistedUser = userRepository.findById(existing.getId()).orElseThrow();

        // then
        assertThat(persistedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(persistedUser.getUserCode()).isEqualTo(EXISTING_CODE);
    }

    @Test
    @DisplayName("코드가 없는 기존 사용자들이 있어도 로그인하며 기존 null 코드를 유지한다")
    void testLogin_allowsLegacyNullCodesWithoutBackfill() {
        // given
        User existing = persistUser("기존 테스트 사용자", null, null, EXISTING_CODE);
        User anotherLegacyUser = persistUser("다른 기존 사용자", null, null, NEW_CODE);
        socialLoginUserRepository.saveAndFlush(SocialLoginUser.link(existing, SocialProvider.TEST, "front-a"));
        jdbcTemplate.update("update users set user_code = null where user_id in (?, ?)",
                existing.getId(), anotherLegacyUser.getId());

        // when
        AuthLoginResponse response = devAuthService.testLogin("front-a");

        // then
        assertThat(response.isNewUser()).isFalse();
        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(userRepository.findAll()).extracting(User::getUserCode).containsOnlyNulls();
        assertThat(rowCounts()).isEqualTo(new RowCounts(2, 1, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 9})
    @DisplayName("개인 코드가 충돌해도 Apple 인증 코드를 재사용하지 않고 열 번째 시도까지 가입할 수 있다")
    void loginWithApple_recoversFromCodeCollisionsWithoutRepeatingCodeExchange(int collisionCount) {
        // given
        User previousUser = persistUser("기존 사용자", null, null, EXISTING_CODE);
        codeGenerator.use(IntStream.rangeClosed(0, collisionCount)
                .mapToObj(index -> index < collisionCount ? EXISTING_CODE : NEW_CODE)
                .toArray(String[]::new));
        prepareAppleLogin();

        // when
        AuthLoginResponse response = authService.loginWithApple(appleRequest());

        // then
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.id()).isNotEqualTo(previousUser.getId());
        assertThat(userRepository.findById(response.id()).orElseThrow().getUserCode()).isEqualTo(NEW_CODE);
        assertThat(userRepository.findById(previousUser.getId()).orElseThrow().getUserCode()).isEqualTo(EXISTING_CODE);
        SocialLoginUser socialLoginUser = socialLoginUserRepository
                .findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-sub").orElseThrow();
        assertThat(providerTokenCipher.decrypt(socialLoginUser.getProviderRefreshToken()))
                .isEqualTo("apple-provider-refresh-token");
        assertThat(rowCounts()).isEqualTo(new RowCounts(2, 1, 1));
    }

    @Test
    @DisplayName("개인 코드가 열 번 충돌하면 503을 반환하고 신규 가입 데이터를 남기지 않는다")
    void testLogin_rejectsTenCollisionsWithoutPartialSignup() {
        // given
        persistUser("기존 사용자", null, null, EXISTING_CODE);
        codeGenerator.use(IntStream.rangeClosed(0, 10)
                .mapToObj(index -> index < 10 ? EXISTING_CODE : NEW_CODE)
                .toArray(String[]::new));

        // when & then
        assertThatThrownBy(() -> devAuthService.testLogin("front-a"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(503);
        assertThat(rowCounts()).isEqualTo(new RowCounts(1, 0, 0));
        assertThat(userRepository.findAll()).extracting(User::getUserCode).containsExactly(EXISTING_CODE);
    }

    @Test
    @DisplayName("세션 토큰 중복은 코드 재시도 없이 409를 반환하고 신규 사용자와 소셜 연결을 롤백한다")
    void testLogin_rollsBackSignupWithoutRetryingUnrelatedUniqueConstraint() {
        // given
        codeGenerator.use(EXISTING_CODE);
        refreshTokenProvider.use("duplicate-refresh-token", "duplicate-refresh-token");
        AuthLoginResponse previous = devAuthService.testLogin("front-a");
        codeGenerator.use(NEW_CODE);

        // when & then
        assertThatThrownBy(() -> devAuthService.testLogin("front-b"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(409);
        assertThat(rowCounts()).isEqualTo(new RowCounts(1, 1, 1));
        assertThat(userRepository.findAll()).extracting(User::getId).containsExactly(previous.id());
        assertThat(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.TEST, "front-b"))
                .isEmpty();
    }

    @Test
    @DisplayName("소셜 연결 저장에 실패하면 먼저 저장한 사용자도 롤백한다")
    void loginWithKakao_rollsBackUserWhenSocialLinkViolatesDatabaseConstraint() {
        // given
        codeGenerator.use(NEW_CODE);
        when(kakaoRestApiClient.getUserInfo("kakao-token"))
                .thenReturn(new KakaoUserInfo("x".repeat(101), "카카오 사용자"));

        // when & then
        assertThatThrownBy(() -> authService.loginWithKakao("kakao-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(409);
        assertThat(rowCounts()).isEqualTo(new RowCounts(0, 0, 0));
    }

    @Test
    @DisplayName("다른 사용자의 이메일로 가입하면 기존 409 계약을 유지하고 가입 데이터를 남기지 않는다")
    void loginWithKakao_preservesEmailConflictBehavior() {
        // given
        User previous = persistUser("기존 사용자", null, "shared@example.com", EXISTING_CODE);
        when(kakaoRestApiClient.getUserInfo("kakao-token"))
                .thenReturn(new KakaoUserInfo("new-kakao", "신규 사용자", "SHARED@EXAMPLE.COM"));

        // when & then
        assertThatThrownBy(() -> authService.loginWithKakao("kakao-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(409);
        assertThat(rowCounts()).isEqualTo(new RowCounts(1, 0, 0));
        assertThat(userRepository.findAll()).extracting(User::getId).containsExactly(previous.getId());
    }

    private AuthLoginResponse loginWithoutEmail(SocialProvider provider) {
        if (provider == SocialProvider.APPLE) {
            prepareAppleLogin();
            return authService.loginWithApple(appleRequest());
        }
        when(kakaoRestApiClient.getUserInfo("kakao-token"))
                .thenReturn(new KakaoUserInfo("new-kakao", "카카오 사용자"));
        return authService.loginWithKakao("kakao-token");
    }

    private void prepareAppleLogin() {
        when(appleIdentityTokenVerifier.verify(APPLE_ID_TOKEN, APPLE_NONCE)).thenReturn("apple-sub");
        when(appleRestApiClient.exchangeAuthorizationCode(APPLE_AUTHORIZATION_CODE))
                .thenReturn("apple-provider-refresh-token")
                .thenThrow(new IllegalStateException("Apple authorization code has already been consumed"));
    }

    private AppleSocialLoginRequest appleRequest() {
        return new AppleSocialLoginRequest(APPLE_ID_TOKEN, APPLE_NONCE, APPLE_AUTHORIZATION_CODE, "애플 사용자");
    }

    private User persistUser(String displayName, String profileImageObjectKey, String email, String userCode) {
        return userRepository.saveAndFlush(User.createNew(displayName, profileImageObjectKey, email, new UserCode(userCode)));
    }

    private RowCounts rowCounts() {
        return new RowCounts(userRepository.count(), socialLoginUserRepository.count(), refreshTokenSessionRepository.count());
    }

    private record RowCounts(long users, long socialLogins, long refreshSessions) {
    }

    static class ScriptedUserCodeGenerator implements UserCodeGenerator {
        private final Deque<UserCode> candidates = new ArrayDeque<>();

        void use(String... codes) {
            candidates.clear();
            Arrays.stream(codes).map(UserCode::new).forEach(candidates::addLast);
        }

        @Override
        public UserCode generate() {
            if (candidates.isEmpty()) {
                throw new AssertionError("예정하지 않은 개인 코드 생성이 발생했습니다.");
            }
            return candidates.removeFirst();
        }
    }

    static class ScriptedRefreshTokenProvider extends RefreshTokenProvider {
        private final Deque<String> tokens = new ArrayDeque<>();
        private int sequence;

        ScriptedRefreshTokenProvider() {
            super(3600);
        }

        void reset() {
            tokens.clear();
            sequence = 0;
        }

        void use(String... values) {
            tokens.clear();
            tokens.addAll(Arrays.asList(values));
        }

        @Override
        public String createRefreshToken() {
            return tokens.isEmpty() ? "refresh-token-" + ++sequence : tokens.removeFirst();
        }
    }

    @TestConfiguration
    static class SignupTestConfig {
        @Bean
        DevAuthService devAuthService(AuthService authService) {
            return new DevAuthService(authService);
        }

        @Bean
        @Primary
        ScriptedUserCodeGenerator scriptedUserCodeGenerator() {
            return new ScriptedUserCodeGenerator();
        }

        @Bean
        @Primary
        ScriptedRefreshTokenProvider scriptedRefreshTokenProvider() {
            return new ScriptedRefreshTokenProvider();
        }
    }
}
