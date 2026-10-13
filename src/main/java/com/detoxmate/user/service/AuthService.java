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
import com.detoxmate.user.infrastructure.persistence.UserCodeCollisionClassifier;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INITIAL_DISPLAY_NAME_LENGTH = 10;
    private static final String APPLE_FALLBACK_DISPLAY_NAME = "AppleUser";
    private static final int MAX_USER_CODE_ATTEMPTS = 10;

    private final KakaoRestApiClient kakaoRestApiClient;
    private final AppleIdentityTokenVerifier appleIdentityTokenVerifier;
    private final AppleRestApiClient appleRestApiClient;
    private final ProviderTokenCipher providerTokenCipher;
    private final UserRepository userRepository;
    private final SocialLoginUserRepository socialLoginUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final UserCodeGenerator userCodeGenerator;
    private final PlatformTransactionManager transactionManager;

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
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (int attempt = 1; attempt <= MAX_USER_CODE_ATTEMPTS; attempt++) {
            try {
                return transaction.execute(status -> loginInTransaction(
                        provider, providerUserId, displayName, profileImageObjectKey,
                        email, encryptedProviderRefreshToken
                ));
            } catch (DataIntegrityViolationException exception) {
                if (!UserCodeCollisionClassifier.isUserCodeCollision(exception)) {
                    throw accountConflict(exception);
                }
                if (attempt == MAX_USER_CODE_ATTEMPTS) {
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "개인 코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                            exception
                    );
                }
            }
        }
        throw new IllegalStateException("User code attempts exhausted");
    }

    private AuthLoginResponse loginInTransaction(
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
                email,
                userCodeGenerator.generate()
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
