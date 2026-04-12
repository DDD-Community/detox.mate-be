package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.dto.KakaoSocialLoginResponse;
import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void 기존_카카오_계정이면_기존_유저로_로그인한다() {
        // given
        FakeKakaoRestApiClient kakaoRestApiClient = new FakeKakaoRestApiClient(
                new KakaoUserInfo("123456789", "카카오닉네임")
        );
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        AuthService authService = new AuthService(kakaoRestApiClient, userRepository, socialLoginUserRepository, jwtTokenProvider);

        User existingUser = User.createNew("기존유저", "https://example.com/existing.png");
        ReflectionTestUtils.setField(existingUser, "id", 7L);
        SocialLoginUser existingSocialLoginUser = SocialLoginUser.link(existingUser, SocialProvider.KAKAO, "123456789");
        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789"))
                .thenReturn(Optional.of(existingSocialLoginUser));

        // when
        KakaoSocialLoginResponse response = authService.loginWithKakao("kakao-access-token");

        // then
        assertThat(kakaoRestApiClient.lastProviderAccessToken()).isEqualTo("kakao-access-token");
        assertThat(response.isNewUser()).isFalse();
        verify(socialLoginUserRepository).findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789");
        verify(userRepository, never()).save(any(User.class));
        verify(socialLoginUserRepository, never()).save(any(SocialLoginUser.class));
    }

    @Test
    void 처음_로그인한_카카오_계정이면_신규_유저를_생성한다() {
        // given
        FakeKakaoRestApiClient kakaoRestApiClient = new FakeKakaoRestApiClient(
                new KakaoUserInfo("123456789", "카카오닉네임")
        );
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        AuthService authService = new AuthService(kakaoRestApiClient, userRepository, socialLoginUserRepository, jwtTokenProvider);

        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            return savedUser;
        });
        when(socialLoginUserRepository.save(any(SocialLoginUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        KakaoSocialLoginResponse response = authService.loginWithKakao("kakao-access-token");

        // then
        assertThat(kakaoRestApiClient.lastProviderAccessToken()).isEqualTo("kakao-access-token");
        assertThat(response.isNewUser()).isTrue();
        verify(socialLoginUserRepository).findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789");
        verify(userRepository).save(any(User.class));
        verify(socialLoginUserRepository).save(any(SocialLoginUser.class));
    }

    private static class FakeKakaoRestApiClient extends KakaoRestApiClient {

        private final KakaoUserInfo kakaoUserInfo;
        private String lastProviderAccessToken;

        private FakeKakaoRestApiClient(KakaoUserInfo kakaoUserInfo) {
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
}
