package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.domain.RefreshTokenSession;
import com.detoxmate.auth.dto.AppleSocialLoginRequest;
import com.detoxmate.auth.dto.AuthLoginResponse;
import com.detoxmate.auth.dto.RefreshTokenResponse;
import com.detoxmate.auth.service.RefreshTokenSessionService;
import com.detoxmate.upload.config.StorageProperties;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.config.KakaoAuthProperties;
import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String JWT_SECRET = "this-is-a-very-long-secret-key-for-temp-auth";
    private static final long ACCESS_TOKEN_EXPIRES_IN = 3600L;
    private static final String TEST_IMAGE_BASE_URL = "https://media.detoxmate.co.kr";

    @Test
    void 기존_카카오_계정이면_기존_유저로_로그인한다() {
        // given
        FakeKakaoRestApiClient kakaoRestApiClient = new FakeKakaoRestApiClient(
                new KakaoUserInfo("123456789", "카카오닉네임")
        );
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        AuthService authService = new AuthService(
                kakaoRestApiClient,
                mock(AppleIdentityTokenVerifier.class),
                mock(AppleRestApiClient.class),
                mock(ProviderTokenCipher.class),
                userRepository,
                socialLoginUserRepository,
                jwtTokenProvider,
                refreshTokenSessionService,
                imageReadUrlBuilder()
        );

        User existingUser = User.createNew("기존유저", "profile-images/7/existing.png");
        ReflectionTestUtils.setField(existingUser, "id", 7L);
        SocialLoginUser existingSocialLoginUser = SocialLoginUser.link(existingUser, SocialProvider.KAKAO, "123456789");
        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789"))
                .thenReturn(Optional.of(existingSocialLoginUser));
        when(refreshTokenSessionService.issueRefreshToken(existingUser)).thenReturn("service-refresh-token");

        // when
        AuthLoginResponse response = authService.loginWithKakao("kakao-access-token");

        // then
        assertThat(kakaoRestApiClient.lastProviderAccessToken()).isEqualTo("kakao-access-token");
        assertThat(response.isNewUser()).isFalse();
        assertThat(response.profileImageUrl()).isEqualTo(TEST_IMAGE_BASE_URL + "/profile-images/7/existing.png");
        assertThat(response.refreshToken()).isEqualTo("service-refresh-token");
        verify(socialLoginUserRepository).findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789");
        verify(userRepository, never()).save(any(User.class));
        verify(socialLoginUserRepository, never()).save(any(SocialLoginUser.class));
    }

    @Test
    void 처음_로그인한_카카오_계정이면_신규_유저를_생성한다() {
        // given
        FakeKakaoRestApiClient kakaoRestApiClient = new FakeKakaoRestApiClient(
                new KakaoUserInfo("123456789", "12345678901")
        );
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        AuthService authService = new AuthService(
                kakaoRestApiClient,
                mock(AppleIdentityTokenVerifier.class),
                mock(AppleRestApiClient.class),
                mock(ProviderTokenCipher.class),
                userRepository,
                socialLoginUserRepository,
                jwtTokenProvider,
                refreshTokenSessionService,
                imageReadUrlBuilder()
        );

        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getDisplayName()).isEqualTo("1234567890");
            assertThat(savedUser.getProfileImageObjectKey()).isNull();
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            return savedUser;
        });
        when(socialLoginUserRepository.save(any(SocialLoginUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenSessionService.issueRefreshToken(any(User.class))).thenReturn("service-refresh-token");

        // when
        AuthLoginResponse response = authService.loginWithKakao("kakao-access-token");

        // then
        assertThat(kakaoRestApiClient.lastProviderAccessToken()).isEqualTo("kakao-access-token");
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.refreshToken()).isEqualTo("service-refresh-token");
        verify(socialLoginUserRepository).findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789");
        verify(userRepository).save(any(User.class));
        verify(socialLoginUserRepository).save(any(SocialLoginUser.class));
    }

    @Test
    @DisplayName("기존 Apple 계정이면 기존 유저로 로그인한다")
    void loginWithApple_returnsExistingUserWhenAppleAccountExists() {
        // given
        AppleIdentityTokenVerifier appleIdentityTokenVerifier = mock(AppleIdentityTokenVerifier.class);
        AppleRestApiClient appleRestApiClient = mock(AppleRestApiClient.class);
        ProviderTokenCipher providerTokenCipher = mock(ProviderTokenCipher.class);
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        AuthService authService = new AuthService(
                mock(KakaoRestApiClient.class),
                appleIdentityTokenVerifier,
                appleRestApiClient,
                providerTokenCipher,
                userRepository,
                socialLoginUserRepository,
                jwtTokenProvider,
                refreshTokenSessionService,
                imageReadUrlBuilder()
        );
        AppleSocialLoginRequest request = new AppleSocialLoginRequest(
                "apple-id-token",
                "apple-raw-nonce",
                "apple-authorization-code",
                "새로받은이름"
        );
        User existingUser = User.createNew("기존애플유저", "profile-images/11/existing.png");
        ReflectionTestUtils.setField(existingUser, "id", 11L);
        SocialLoginUser existingSocialLoginUser = SocialLoginUser.link(existingUser, SocialProvider.APPLE, "apple-sub-123");

        when(appleIdentityTokenVerifier.verify("apple-id-token", "apple-raw-nonce"))
                .thenReturn("apple-sub-123");
        when(appleRestApiClient.exchangeAuthorizationCode("apple-authorization-code"))
                .thenReturn("apple-refresh-token");
        when(providerTokenCipher.encrypt("apple-refresh-token"))
                .thenReturn("encrypted-apple-refresh-token");
        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-sub-123"))
                .thenReturn(Optional.of(existingSocialLoginUser));
        when(refreshTokenSessionService.issueRefreshToken(existingUser)).thenReturn("service-refresh-token");

        // when
        AuthLoginResponse response = authService.loginWithApple(request);

        // then
        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.displayName()).isEqualTo("기존애플유저");
        assertThat(response.isNewUser()).isFalse();
        assertThat(response.profileImageUrl()).isEqualTo(TEST_IMAGE_BASE_URL + "/profile-images/11/existing.png");
        assertThat(response.refreshToken()).isEqualTo("service-refresh-token");
        assertThat(existingSocialLoginUser.getProviderRefreshToken()).isEqualTo("encrypted-apple-refresh-token");
        verify(socialLoginUserRepository).findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-sub-123");
        verify(userRepository, never()).save(any(User.class));
        verify(socialLoginUserRepository, never()).save(any(SocialLoginUser.class));
        verify(appleIdentityTokenVerifier).verify("apple-id-token", "apple-raw-nonce");
    }

    @Test
    @DisplayName("처음 로그인한 Apple 계정이면 신규 유저와 Apple 소셜 로그인을 생성한다")
    void loginWithApple_createsUserAndSocialLoginWhenAppleAccountIsNew() {
        // given
        AppleIdentityTokenVerifier appleIdentityTokenVerifier = mock(AppleIdentityTokenVerifier.class);
        AppleRestApiClient appleRestApiClient = mock(AppleRestApiClient.class);
        ProviderTokenCipher providerTokenCipher = mock(ProviderTokenCipher.class);
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        AuthService authService = new AuthService(
                mock(KakaoRestApiClient.class),
                appleIdentityTokenVerifier,
                appleRestApiClient,
                providerTokenCipher,
                userRepository,
                socialLoginUserRepository,
                jwtTokenProvider,
                refreshTokenSessionService,
                imageReadUrlBuilder()
        );
        AppleSocialLoginRequest request = new AppleSocialLoginRequest(
                "apple-id-token",
                "apple-raw-nonce",
                "apple-authorization-code",
                "애플닉네임123456"
        );

        when(appleIdentityTokenVerifier.verify("apple-id-token", "apple-raw-nonce"))
                .thenReturn("apple-sub-456");
        when(appleRestApiClient.exchangeAuthorizationCode("apple-authorization-code"))
                .thenReturn("apple-refresh-token");
        when(providerTokenCipher.encrypt("apple-refresh-token"))
                .thenReturn("encrypted-apple-refresh-token");
        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-sub-456"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getDisplayName()).isEqualTo("애플닉네임12345");
            assertThat(savedUser.getProfileImageObjectKey()).isNull();
            ReflectionTestUtils.setField(savedUser, "id", 12L);
            return savedUser;
        });
        when(socialLoginUserRepository.save(any(SocialLoginUser.class))).thenAnswer(invocation -> {
            SocialLoginUser savedSocialLoginUser = invocation.getArgument(0);
            assertThat(savedSocialLoginUser.getProvider()).isEqualTo(SocialProvider.APPLE);
            assertThat(savedSocialLoginUser.getProviderUserId()).isEqualTo("apple-sub-456");
            assertThat(savedSocialLoginUser.getProviderRefreshToken()).isEqualTo("encrypted-apple-refresh-token");
            return savedSocialLoginUser;
        });
        when(refreshTokenSessionService.issueRefreshToken(any(User.class))).thenReturn("service-refresh-token");

        // when
        AuthLoginResponse response = authService.loginWithApple(request);

        // then
        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.refreshToken()).isEqualTo("service-refresh-token");
        verify(userRepository).save(any(User.class));
        verify(socialLoginUserRepository).save(any(SocialLoginUser.class));
    }

    @Test
    @DisplayName("처음 로그인한 Apple 계정의 displayName이 없으면 기본 이름을 사용한다")
    void loginWithApple_usesFallbackDisplayNameWhenDisplayNameIsMissing() {
        // given
        AppleIdentityTokenVerifier appleIdentityTokenVerifier = mock(AppleIdentityTokenVerifier.class);
        AppleRestApiClient appleRestApiClient = mock(AppleRestApiClient.class);
        ProviderTokenCipher providerTokenCipher = mock(ProviderTokenCipher.class);
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        AuthService authService = new AuthService(
                mock(KakaoRestApiClient.class),
                appleIdentityTokenVerifier,
                appleRestApiClient,
                providerTokenCipher,
                userRepository,
                socialLoginUserRepository,
                jwtTokenProvider,
                refreshTokenSessionService,
                imageReadUrlBuilder()
        );
        AppleSocialLoginRequest request = new AppleSocialLoginRequest(
                "apple-id-token",
                "apple-raw-nonce",
                "apple-authorization-code",
                null
        );

        when(appleIdentityTokenVerifier.verify("apple-id-token", "apple-raw-nonce"))
                .thenReturn("apple-sub-789");
        when(appleRestApiClient.exchangeAuthorizationCode("apple-authorization-code"))
                .thenReturn("apple-refresh-token");
        when(providerTokenCipher.encrypt("apple-refresh-token"))
                .thenReturn("encrypted-apple-refresh-token");
        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-sub-789"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getDisplayName()).isEqualTo("AppleUser");
            ReflectionTestUtils.setField(savedUser, "id", 13L);
            return savedUser;
        });
        when(socialLoginUserRepository.save(any(SocialLoginUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenSessionService.issueRefreshToken(any(User.class))).thenReturn("service-refresh-token");

        // when
        AuthLoginResponse response = authService.loginWithApple(request);

        // then
        assertThat(response.id()).isEqualTo(13L);
        assertThat(response.displayName()).isEqualTo("AppleUser");
        assertThat(response.isNewUser()).isTrue();
    }

    @Test
    void 유효한_refresh_token이면_새_access_token과_새_refresh_token을_발급한다() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        AuthService authService = new AuthService(
                mock(KakaoRestApiClient.class),
                mock(AppleIdentityTokenVerifier.class),
                mock(AppleRestApiClient.class),
                mock(ProviderTokenCipher.class),
                userRepository,
                socialLoginUserRepository,
                jwtTokenProvider,
                refreshTokenSessionService,
                imageReadUrlBuilder()
        );
        User user = User.createNew("카카오닉네임");
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshTokenSession refreshTokenSession = RefreshTokenSession.issue(
                user,
                "hashed-refresh-token",
                java.time.LocalDateTime.now().plusDays(180)
        );

        when(refreshTokenSessionService.getValidSession("valid-refresh-token"))
                .thenReturn(refreshTokenSession);
        when(refreshTokenSessionService.issueRefreshToken(user))
                .thenReturn("rotated-refresh-token");

        // when
        RefreshTokenResponse response = authService.refresh("valid-refresh-token");

        // then
        assertThat(response.refreshToken()).isEqualTo("rotated-refresh-token");
        assertThat(jwtTokenProvider.getUserId(response.accessToken())).isEqualTo(1L);
        verify(refreshTokenSessionService).getValidSession("valid-refresh-token");
        verify(refreshTokenSessionService).issueRefreshToken(user);
        verify(refreshTokenSessionService).revoke("valid-refresh-token");
    }

    @Test
    void 유효한_refresh_token이면_로그아웃시_세션을_revoke한다() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        AuthService authService = new AuthService(
                mock(KakaoRestApiClient.class),
                mock(AppleIdentityTokenVerifier.class),
                mock(AppleRestApiClient.class),
                mock(ProviderTokenCipher.class),
                userRepository,
                socialLoginUserRepository,
                jwtTokenProvider,
                refreshTokenSessionService,
                imageReadUrlBuilder()
        );

        // when
        authService.logout("valid-refresh-token");

        // then
        verify(refreshTokenSessionService).revoke("valid-refresh-token");
    }

    private static class FakeKakaoRestApiClient extends KakaoRestApiClient {

        private final KakaoUserInfo kakaoUserInfo;
        private String lastProviderAccessToken;

        private FakeKakaoRestApiClient(KakaoUserInfo kakaoUserInfo) {
            super(RestClient.builder().baseUrl("https://kapi.kakao.com").build(), new KakaoAuthProperties(""));
            this.kakaoUserInfo = kakaoUserInfo;
        }

        @Override
        public KakaoUserInfo getUserInfo(String providerAccessToken) {
            this.lastProviderAccessToken = providerAccessToken;
            return kakaoUserInfo;
        }

        private String lastProviderAccessToken() {
            return lastProviderAccessToken;
        }
    }

    private ImageReadUrlBuilder imageReadUrlBuilder() {
        return new ImageReadUrlBuilder(new StorageProperties(TEST_IMAGE_BASE_URL));
    }
}
