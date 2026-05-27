package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.service.RefreshTokenSessionService;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.service.GroupMemberService;
import com.detoxmate.group.service.GroupService;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.upload.dto.UploadPurpose;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.upload.service.UploadObjectKeyValidator;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.dto.UpdateMyProfileRequest;
import com.detoxmate.user.dto.UserProfileSummary;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
    private final GroupMemberService groupMemberService;
    private final GroupService groupService;
    private final FcmTokenRepository fcmTokenRepository;
    private final ProviderAccountDisconnectService providerAccountDisconnectService;

    @Transactional(readOnly = true)
    public MyProfileResponse getMe(String accessToken) {
        User user = getActiveUser(accessToken);

        return toMyProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMe(Long userId) {
        User user = getActiveUser(userId);

        return toMyProfileResponse(user);
    }

    @Transactional
    public MyProfileResponse updateMe(Long userId, UpdateMyProfileRequest request) {
        User user = getActiveUser(userId);

        if (request.displayName() != null) {
            user.changeDisplayName(request.displayName());
        }
        if (request.profileImageObjectKey() != null) {
            validateProfileImageObjectKey(userId, request.profileImageObjectKey());
            user.changeProfileImageObjectKey(request.profileImageObjectKey());
        }

        return toMyProfileResponse(user);
    }

    @Transactional
    public void withdrawMe(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow();

        if (user.isWithdrawn()) {
            return;
        }

        var socialLoginUsers = socialLoginUserRepository.findAllByUserId(userId);
        socialLoginUsers.forEach(providerAccountDisconnectService::disconnect);

        List<Long> activeGroupIds = groupMemberService.getActiveGroupMembers(userId).stream()
                .map(GroupMember::getGroupId)
                .toList();

        activeGroupIds.forEach(groupId -> groupService.withdrawGroup(groupId, userId));

        socialLoginUserRepository.deleteByUserId(userId);
        refreshTokenSessionService.deleteByUserId(userId);
        fcmTokenRepository.deleteByUserId(userId);
        user.withdraw();
    }

    @Transactional
    public void withdraw(String accessToken) {
        Long userId = jwtTokenProvider.getUserId(accessToken);
        withdrawMe(userId);
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

    @Transactional(readOnly = true)
    public Map<Long, UserProfileSummary> getProfileSummariesByIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::toUserProfileSummary
                ));
    }

    @Transactional
    public void updatePushNotificationSetting(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.updatePushNotificationEnabled(enabled);
    }

    private User getUser(String accessToken) {
        Long userId = jwtTokenProvider.getUserId(accessToken);
        return getUser(userId);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow();
    }

    private User getActiveUser(String accessToken) {
        return validateActive(getUser(accessToken));
    }

    private User getActiveUser(Long userId) {
        return validateActive(getUser(userId));
    }

    private User validateActive(User user) {
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "탈퇴한 사용자입니다.");
        }

        return user;
    }

    private MyProfileResponse toMyProfileResponse(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getPublicDisplayName(),
                imageReadUrlBuilder.build(user.getPublicProfileImageObjectKey()),
                user.isPushNotificationEnabled()
        );
    }

    private UserProfileSummary toUserProfileSummary(User user) {
        return new UserProfileSummary(
                user.getId(),
                user.getPublicDisplayName(),
                imageReadUrlBuilder.build(user.getPublicProfileImageObjectKey()),
                user.isWithdrawn()
        );
    }

    private void validateProfileImageObjectKey(Long userId, String profileImageObjectKey) {
        if (!uploadObjectKeyValidator.isOwnedBy(userId, UploadPurpose.PROFILE_IMAGE, profileImageObjectKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 이미지 object key 경로가 올바르지 않습니다.");
        }
    }
}
