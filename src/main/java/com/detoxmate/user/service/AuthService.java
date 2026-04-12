package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.dto.KakaoSocialLoginResponse;
import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoRestApiClient kakaoRestApiClient;
    private final UserRepository userRepository;
    private final SocialLoginUserRepository socialLoginUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public KakaoSocialLoginResponse loginWithKakao(String providerAccessToken) {
        KakaoUserInfo kakaoUserInfo = kakaoRestApiClient.getUserInfo(providerAccessToken);
        Optional<SocialLoginUser> existingSocialLoginUser = socialLoginUserRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                kakaoUserInfo.providerUserId()
        );
        boolean isNewUser = existingSocialLoginUser.isEmpty();
        SocialLoginUser socialLoginUser = existingSocialLoginUser.orElseGet(() -> createNewSocialLoginUser(kakaoUserInfo));
        User user = socialLoginUser.getUser();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        return new KakaoSocialLoginResponse(
                user.getId(),
                user.getDisplayName(),
                user.getProfileImageUrl(),
                accessToken,
                jwtTokenProvider.getAccessTokenExpiresIn(),
                isNewUser
        );
    }

    private SocialLoginUser createNewSocialLoginUser(KakaoUserInfo kakaoUserInfo) {
        User newUser = userRepository.save(User.createNew(kakaoUserInfo.nickname()));
        SocialLoginUser socialLoginUser = SocialLoginUser.link(newUser, SocialProvider.KAKAO, kakaoUserInfo.providerUserId());
        return socialLoginUserRepository.save(socialLoginUser);
    }
}
