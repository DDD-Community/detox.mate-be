package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String JWT_SECRET = "this-is-a-very-long-secret-key-for-temp-auth";
    private static final long ACCESS_TOKEN_EXPIRES_IN = 3600L;

    @Test
    void accessToken으로_현재_유저를_조회하여_프로필_응답을_반환한다() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = new UserService(userRepository, socialLoginUserRepository, jwtTokenProvider);

        User user = User.createNew("카카오닉네임", "https://example.com/profile.png");
        ReflectionTestUtils.setField(user, "id", 1L);
        String accessToken = jwtTokenProvider.createAccessToken(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        MyProfileResponse response = userService.getMe(accessToken);

        // then
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void accessToken에_해당하는_유저가_없으면_예외를_던진다() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = new UserService(userRepository, socialLoginUserRepository, jwtTokenProvider);
        String accessToken = jwtTokenProvider.createAccessToken(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMe(accessToken))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void accessToken으로_현재_유저를_조회하여_회원_탈퇴를_수행한다() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = new UserService(userRepository, socialLoginUserRepository, jwtTokenProvider);

        User user = User.createNew("카카오닉네임", "https://example.com/profile.png");
        ReflectionTestUtils.setField(user, "id", 1L);
        String accessToken = jwtTokenProvider.createAccessToken(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        userService.withdraw(accessToken);

        // then
        InOrder inOrder = inOrder(socialLoginUserRepository, userRepository);
        inOrder.verify(socialLoginUserRepository).deleteByUserId(1L);
        inOrder.verify(userRepository).delete(user);
    }

    @Test
    void 탈퇴할_accessToken에_해당하는_유저가_없으면_예외를_던진다() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = new UserService(userRepository, socialLoginUserRepository, jwtTokenProvider);
        String accessToken = jwtTokenProvider.createAccessToken(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.withdraw(accessToken))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
