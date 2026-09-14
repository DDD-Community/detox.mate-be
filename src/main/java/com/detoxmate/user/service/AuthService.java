package com.detoxmate.user.service;

import com.detoxmate.auth.dto.AppleSocialLoginRequest;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.domain.RefreshTokenSession;
import com.detoxmate.auth.dto.AuthLoginResponse;
import com.detoxmate.auth.dto.RefreshTokenResponse;
import com.detoxmate.auth.service.RefreshTokenSessionService;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.domain.SocialLoginUser;
import com.detoxmate.user.domain.SocialProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INITIAL_DISPLAY_NAME_LENGTH = 10;
    private static final String APPLE_FALLBACK_DISPLAY_NAME = "AppleUser";

    private final KakaoRestApiClient kakaoRestApiClient;
    private final AppleIdentityTokenVerifier appleIdentityTokenVerifier;
    private final AppleRestApiClient appleRestApiClient;
    private final ProviderTokenCipher providerTokenCipher;
    private final UserRepository userRepository;
    private final SocialLoginUserRepository socialLoginUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final ImageReadUrlBuilder imageReadUrlBuilder;

    @Transactional
    public AuthLoginResponse loginWithKakao(String providerAccessToken) {
        KakaoUserInfo kakaoUserInfo = kakaoRestApiClient.getUserInfo(providerAccessToken);
        return loginWithSocialUser(
                SocialProvider.KAKAO,
                kakaoUserInfo.providerUserId(),
                kakaoUserInfo.nickname(),
                null,
                kakaoUserInfo.email(),
                null
        );
    }

    @Transactional
    public AuthLoginResponse loginWithApple(AppleSocialLoginRequest request) {
        String providerUserId = appleIdentityTokenVerifier.verify(request.identityToken(), request.rawNonce());
        String email = appleIdentityTokenVerifier.extractEmail(request.identityToken(), request.rawNonce());
        String providerRefreshToken = appleRestApiClient.exchangeAuthorizationCode(request.authorizationCode());
        String encryptedProviderRefreshToken = providerTokenCipher.encrypt(providerRefreshToken);

        return loginWithSocialUser(
                SocialProvider.APPLE,
                providerUserId,
                resolveInitialAppleDisplayName(request.displayName()),
                null,
                email,
                encryptedProviderRefreshToken
        );
    }

    AuthLoginResponse loginWithSocialUser(
            SocialProvider provider,
            String providerUserId,
            String displayName,
            String profileImageObjectKey
    ) {
        return loginWithSocialUser(provider, providerUserId, displayName, profileImageObjectKey, null, null);
    }

    AuthLoginResponse loginWithSocialUser(
            SocialProvider provider,
            String providerUserId,
            String displayName,
            String profileImageObjectKey,
            String email,
            String encryptedProviderRefreshToken
    ) {
        Optional<SocialLoginUser> existingSocialLoginUser = socialLoginUserRepository.findByProviderAndProviderUserId(
                provider,
                providerUserId
        );
        User existingUser = existingSocialLoginUser.map(SocialLoginUser::getUser).orElse(null);
        validateEmailAvailability(existingUser, email);

        try {
            boolean isNewUser = existingSocialLoginUser.isEmpty();
            SocialLoginUser socialLoginUser = existingSocialLoginUser.orElseGet(
                    () -> createNewSocialLoginUser(
                            provider,
                            providerUserId,
                            displayName,
                            profileImageObjectKey,
                            email,
                            encryptedProviderRefreshToken
                    )
            );
            if (existingSocialLoginUser.isPresent() && encryptedProviderRefreshToken != null) {
                socialLoginUser.updateProviderRefreshToken(encryptedProviderRefreshToken);
            }
            User user = socialLoginUser.getUser();
            if (user.registerEmailIfAbsent(email)) {
                userRepository.saveAndFlush(user);
            }
            String accessToken = jwtTokenProvider.createAccessToken(user.getId());
            String refreshToken = refreshTokenSessionService.issueRefreshToken(user);

            return new AuthLoginResponse(
                    user.getId(),
                    user.getDisplayName(),
                    imageReadUrlBuilder.build(user.getProfileImageObjectKey()),
                    accessToken,
                    refreshToken,
                    isNewUser
            );
        } catch (DataIntegrityViolationException exception) {
            throw accountConflict(exception);
        }
    }

    private SocialLoginUser createNewSocialLoginUser(
            SocialProvider provider,
            String providerUserId,
            String displayName,
            String profileImageObjectKey,
            String email,
            String encryptedProviderRefreshToken
    ) {
        User newUser = userRepository.saveAndFlush(User.createNew(
                truncateDisplayName(displayName),
                profileImageObjectKey,
                email
        ));
        SocialLoginUser socialLoginUser = SocialLoginUser.link(newUser, provider, providerUserId);
        if (encryptedProviderRefreshToken != null) {
            socialLoginUser.updateProviderRefreshToken(encryptedProviderRefreshToken);
        }
        return socialLoginUserRepository.saveAndFlush(socialLoginUser);
    }

    private void validateEmailAvailability(User currentUser, String email) {
        String normalizedEmail = User.normalizeEmail(email);
        if (normalizedEmail == null) {
            return;
        }

        userRepository.findByEmail(normalizedEmail)
                .filter(user -> currentUser == null || !user.getId().equals(currentUser.getId()))
                .ifPresent(user -> {
                    throw emailConflict(null);
                });
    }

    private ResponseStatusException emailConflict(Throwable cause) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 다른 계정에서 사용하는 이메일입니다.",
                cause
        );
    }

    private ResponseStatusException accountConflict(Throwable cause) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 등록된 계정 정보입니다.",
                cause
        );
    }

    private String truncateDisplayName(String displayName) {
        if (displayName == null || displayName.length() <= MAX_INITIAL_DISPLAY_NAME_LENGTH) {
            return displayName;
        }

        return displayName.substring(0, MAX_INITIAL_DISPLAY_NAME_LENGTH);
    }

    private String resolveInitialAppleDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return APPLE_FALLBACK_DISPLAY_NAME;
        }

        return displayName;
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
