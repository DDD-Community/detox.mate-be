package com.detoxmate.user.service;

import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderAccountDisconnectServiceTest {

    @Test
    @DisplayName("Kakao 계정은 저장된 providerUserId로 연결을 해제한다")
    void disconnect_unlinksKakaoAccountWithProviderUserId() {
        // given
        KakaoRestApiClient kakaoRestApiClient = mock(KakaoRestApiClient.class);
        ProviderAccountDisconnectService service = new ProviderAccountDisconnectService(
                kakaoRestApiClient,
                mock(AppleRestApiClient.class),
                mock(ProviderTokenCipher.class)
        );
        SocialLoginUser socialLoginUser = SocialLoginUser.link(
                User.createNew("카카오닉네임"),
                SocialProvider.KAKAO,
                "123456789"
        );

        // when
        service.disconnect(socialLoginUser);

        // then
        verify(kakaoRestApiClient).unlinkByAdminKey("123456789");
    }

    @Test
    @DisplayName("Apple 계정은 저장된 refresh token을 복호화해 revoke한다")
    void disconnect_revokesAppleRefreshToken() {
        // given
        AppleRestApiClient appleRestApiClient = mock(AppleRestApiClient.class);
        ProviderTokenCipher providerTokenCipher = mock(ProviderTokenCipher.class);
        ProviderAccountDisconnectService service = new ProviderAccountDisconnectService(
                mock(KakaoRestApiClient.class),
                appleRestApiClient,
                providerTokenCipher
        );
        SocialLoginUser socialLoginUser = SocialLoginUser.link(
                User.createNew("애플유저"),
                SocialProvider.APPLE,
                "apple-sub"
        );
        socialLoginUser.updateProviderRefreshToken("encrypted-refresh-token");

        when(providerTokenCipher.decrypt("encrypted-refresh-token")).thenReturn("apple-refresh-token");

        // when
        service.disconnect(socialLoginUser);

        // then
        verify(appleRestApiClient).revokeRefreshToken("apple-refresh-token");
    }
}
