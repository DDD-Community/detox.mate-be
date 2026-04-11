package com.detoxmate.user.service;

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

    @Test
    void loginWithKakaoReturnsExistingUserWhenSocialLoginUserExists() {
        FakeKakaoRestApiClient kakaoRestApiClient = new FakeKakaoRestApiClient(new KakaoUserInfo("123456789", "카카오닉네임"));
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        AuthService authService = new AuthService(kakaoRestApiClient, userRepository, socialLoginUserRepository);

        User existingUser = User.createNew("기존유저");
        ReflectionTestUtils.setField(existingUser, "id", 7L);
        SocialLoginUser existingSocialLoginUser = SocialLoginUser.link(existingUser, SocialProvider.KAKAO, "123456789");
        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789"))
                .thenReturn(Optional.of(existingSocialLoginUser));

        KakaoSocialLoginResponse response = authService.loginWithKakao("kakao-access-token");

        assertThat(kakaoRestApiClient.lastProviderAccessToken()).isEqualTo("kakao-access-token");
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.displayName()).isEqualTo("기존유저");
        assertThat(response.accessToken()).isEqualTo("service-access-token");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(3600L);
        assertThat(response.isNewUser()).isFalse();
        verify(userRepository, never()).save(any(User.class));
        verify(socialLoginUserRepository, never()).save(any(SocialLoginUser.class));
    }

    @Test
    void loginWithKakaoCreatesNewUserWhenSocialLoginUserDoesNotExist() {
        FakeKakaoRestApiClient kakaoRestApiClient = new FakeKakaoRestApiClient(new KakaoUserInfo("123456789", "카카오닉네임"));
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        AuthService authService = new AuthService(kakaoRestApiClient, userRepository, socialLoginUserRepository);

        when(socialLoginUserRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "123456789"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            return savedUser;
        });
        when(socialLoginUserRepository.save(any(SocialLoginUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KakaoSocialLoginResponse response = authService.loginWithKakao("kakao-access-token");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.displayName()).isEqualTo("카카오닉네임");
        assertThat(response.isNewUser()).isTrue();
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
