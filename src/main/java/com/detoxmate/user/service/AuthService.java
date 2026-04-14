package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.domain.RefreshTokenSession;
import com.detoxmate.auth.dto.KakaoSocialLoginResponse;
import com.detoxmate.auth.dto.RefreshTokenResponse;
import com.detoxmate.auth.service.RefreshTokenSessionService;
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
    private final RefreshTokenSessionService refreshTokenSessionService;

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
        String refreshToken = refreshTokenSessionService.issueRefreshToken(user);

        return new KakaoSocialLoginResponse(
                user.getId(),
                user.getDisplayName(),
                user.getProfileImageUrl(),
                accessToken,
                refreshToken,
                isNewUser
        );
    }

    private SocialLoginUser createNewSocialLoginUser(KakaoUserInfo kakaoUserInfo) {
        User newUser = userRepository.save(User.createNew(kakaoUserInfo.nickname()));
        SocialLoginUser socialLoginUser = SocialLoginUser.link(newUser, SocialProvider.KAKAO, kakaoUserInfo.providerUserId());
        return socialLoginUserRepository.save(socialLoginUser);
    }

    @Transactional
    public RefreshTokenResponse refresh(String refreshToken) {
        RefreshTokenSession refreshTokenSession = refreshTokenSessionService.getValidSession(refreshToken);
        refreshTokenSession.markUsed();
        String newRefreshToken = refreshTokenSessionService.issueRefreshToken(refreshTokenSession.getUser());
        refreshTokenSessionService.revoke(refreshToken);

        String accessToken = jwtTokenProvider.createAccessToken(refreshTokenSession.getUser().getId());

        return new RefreshTokenResponse(accessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenSessionService.revoke(refreshToken);
    }
}
