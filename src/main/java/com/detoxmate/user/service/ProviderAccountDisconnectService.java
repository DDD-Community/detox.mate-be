package com.detoxmate.user.service;

import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProviderAccountDisconnectService {

    private final KakaoRestApiClient kakaoRestApiClient;
    private final AppleRestApiClient appleRestApiClient;
    private final ProviderTokenCipher providerTokenCipher;

    public void disconnect(SocialLoginUser socialLoginUser) {
        if (socialLoginUser.getProvider() == SocialProvider.KAKAO) {
            kakaoRestApiClient.unlinkByAdminKey(socialLoginUser.getProviderUserId());
            return;
        }

        if (socialLoginUser.getProvider() == SocialProvider.APPLE) {
            String refreshToken = providerTokenCipher.decrypt(socialLoginUser.getProviderRefreshToken());
            appleRestApiClient.revokeRefreshToken(refreshToken);
            return;
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported social provider");
    }
}
