package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.service.RefreshTokenSessionService;
import com.detoxmate.upload.dto.UploadPurpose;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.upload.service.UploadObjectKeyValidator;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.dto.UpdateMyProfileRequest;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final SocialLoginUserRepository socialLoginUserRepository;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final UploadObjectKeyValidator uploadObjectKeyValidator;

    @Transactional(readOnly = true)
    public MyProfileResponse getMe(String accessToken) {
        User user = getUser(accessToken);

        return toMyProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMe(Long userId) {
        User user = getUser(userId);

        return toMyProfileResponse(user);
    }

    @Transactional
    public MyProfileResponse updateMe(Long userId, UpdateMyProfileRequest request) {
        User user = getUser(userId);

        if (request.displayName() != null) {
            user.changeDisplayName(request.displayName());
        }
        if (request.profileImageObjectKey() != null) {
            validateProfileImageObjectKey(userId, request.profileImageObjectKey());
            user.changeProfileImageObjectKey(request.profileImageObjectKey());
        }

        return toMyProfileResponse(user);
    }

    public void withdrawMe(Long userId) {
        throw new UnsupportedOperationException("아직 미구현 - API 문서화 단계");
    }

    @Transactional
    public void withdraw(String accessToken) {
        User user = getUser(accessToken);

        socialLoginUserRepository.deleteByUserId(user.getId());
        refreshTokenSessionService.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public Map<Long, MyProfileResponse> getProfilesByIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::toMyProfileResponse
                ));
    }

    private User getUser(String accessToken) {
        Long userId = jwtTokenProvider.getUserId(accessToken);
        return getUser(userId);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow();
    }

    private MyProfileResponse toMyProfileResponse(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getDisplayName(),
                imageReadUrlBuilder.build(user.getProfileImageObjectKey())
        );
    }

    private void validateProfileImageObjectKey(Long userId, String profileImageObjectKey) {
        if (!uploadObjectKeyValidator.isOwnedBy(userId, UploadPurpose.PROFILE_IMAGE, profileImageObjectKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 이미지 object key 경로가 올바르지 않습니다.");
        }
    }
}
