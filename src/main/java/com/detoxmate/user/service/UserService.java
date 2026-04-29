package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.service.RefreshTokenSessionService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                user.getProfileImageUrl()
        );
    }
}
