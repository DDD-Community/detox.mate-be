package com.detoxmate.user.service;

import com.detoxmate.auth.dto.KakaoSocialLoginResponse;
import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoRestApiClient kakaoRestApiClient;
    private final UserRepository userRepository;
    private final SocialLoginUserRepository socialLoginUserRepository;

    public KakaoSocialLoginResponse loginWithKakao(String providerAccessToken) {
        KakaoUserInfo kakaoUserInfo = kakaoRestApiClient.getUserInfo(providerAccessToken);
        SocialLoginUser socialLoginUser = socialLoginUserRepository.findByProviderAndProviderUserId(
                        SocialProvider.KAKAO,
                        kakaoUserInfo.providerUserId()
                )
                .orElseGet(() -> createNewSocialLoginUser(kakaoUserInfo));
        boolean isNewUser = socialLoginUserRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                kakaoUserInfo.providerUserId()
        ).isEmpty();
        User user = socialLoginUser.getUser();

        return new KakaoSocialLoginResponse(
                user.getId(),
                user.getDisplayName(),
                "service-access-token",
                3600L,
                isNewUser
        );
    }

    private SocialLoginUser createNewSocialLoginUser(KakaoUserInfo kakaoUserInfo) {
        User newUser = userRepository.save(User.createNew(kakaoUserInfo.nickname()));
        SocialLoginUser socialLoginUser = SocialLoginUser.link(newUser, SocialProvider.KAKAO, kakaoUserInfo.providerUserId());
        return socialLoginUserRepository.save(socialLoginUser);
    }
}
