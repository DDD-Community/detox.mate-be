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

        return new MyProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getProfileImageUrl()
        );
    }

    @Transactional
    public void withdraw(String accessToken) {
        User user = getUser(accessToken);

        socialLoginUserRepository.deleteByUserId(user.getId());
        refreshTokenSessionService.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    private User getUser(String accessToken) {
        Long userId = jwtTokenProvider.getUserId(accessToken);
        return userRepository.findById(userId)
                .orElseThrow();
    }
}
