package com.detoxmate.user.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.auth.service.RefreshTokenSessionService;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.service.GroupMemberService;
import com.detoxmate.group.service.GroupService;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.upload.config.StorageProperties;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.upload.service.ProfileImageUploadPurposePolicy;
import com.detoxmate.upload.service.UploadObjectKeyValidator;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.dto.UpdateMyProfileRequest;
import com.detoxmate.user.repository.SocialLoginUserRepository;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String JWT_SECRET = "this-is-a-very-long-secret-key-for-temp-auth";
    private static final long ACCESS_TOKEN_EXPIRES_IN = 3600L;
    private static final String TEST_IMAGE_BASE_URL = "https://media.detoxmate.co.kr";

    @Test
    @DisplayName("accessToken으로 현재 유저를 조회하여 프로필 응답을 반환한다")
    void getMe_returnsProfileResponseForAccessToken() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);

        User user = User.createNew("카카오닉네임", "profile-images/1/profile.png");
        ReflectionTestUtils.setField(user, "id", 1L);
        String accessToken = jwtTokenProvider.createAccessToken(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        MyProfileResponse response = userService.getMe(accessToken);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.displayName()).isEqualTo("카카오닉네임");
        assertThat(response.profileImageUrl()).isEqualTo(TEST_IMAGE_BASE_URL + "/profile-images/1/profile.png");
    }

    @Test
    @DisplayName("accessToken에 해당하는 유저가 없으면 예외를 던진다")
    void getMe_throwsExceptionWhenUserDoesNotExist() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);
        String accessToken = jwtTokenProvider.createAccessToken(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMe(accessToken))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("탈퇴한 유저의 accessToken으로 조회하면 401 예외를 던진다")
    void getMe_throwsUnauthorizedWhenUserIsWithdrawn() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);
        String accessToken = jwtTokenProvider.createAccessToken(1L);
        User user = User.createNew("카카오닉네임", "profile-images/1/profile.png");
        user.withdraw();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.getMe(accessToken))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    @Test
    @DisplayName("displayName과 profileImageObjectKey를 함께 변경한다")
    void updateMe_changesDisplayNameAndProfileImageObjectKey() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);
        User user = User.createNew("카카오닉네임", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        MyProfileResponse response = userService.updateMe(
                1L,
                new UpdateMyProfileRequest("의진", "profile-images/1/updated.png")
        );

        // then
        assertThat(user.getDisplayName()).isEqualTo("의진");
        assertThat(user.getProfileImageObjectKey()).isEqualTo("profile-images/1/updated.png");
        assertThat(response.profileImageUrl()).isEqualTo(TEST_IMAGE_BASE_URL + "/profile-images/1/updated.png");
    }

    @Test
    @DisplayName("다른 사용자 prefix의 profileImageObjectKey는 거부한다")
    void updateMe_rejectsProfileImageObjectKeyWithDifferentUserPrefix() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);
        User user = User.createNew("카카오닉네임", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.updateMe(
                1L,
                new UpdateMyProfileRequest(null, "profile-images/2/updated.png")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    @DisplayName("accessToken으로 현재 유저를 조회하여 회원 탈퇴를 수행한다")
    void withdraw_withdrawsCurrentUserForAccessToken() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        GroupMemberService groupMemberService = mock(GroupMemberService.class);
        GroupService groupService = mock(GroupService.class);
        FcmTokenRepository fcmTokenRepository = mock(FcmTokenRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = new UserService(
                userRepository,
                socialLoginUserRepository,
                refreshTokenSessionService,
                jwtTokenProvider,
                new ImageReadUrlBuilder(new StorageProperties(TEST_IMAGE_BASE_URL)),
                uploadObjectKeyValidator(),
                groupMemberService,
                groupService,
                fcmTokenRepository
        );

        User user = User.createNew("카카오닉네임", "profile-images/1/profile.png");
        ReflectionTestUtils.setField(user, "id", 1L);
        String accessToken = jwtTokenProvider.createAccessToken(1L);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(groupMemberService.getActiveGroupMembers(1L)).thenReturn(List.of());

        // when
        userService.withdraw(accessToken);

        // then
        InOrder inOrder = inOrder(socialLoginUserRepository, refreshTokenSessionService, fcmTokenRepository);
        inOrder.verify(socialLoginUserRepository).deleteByUserId(1L);
        inOrder.verify(refreshTokenSessionService).deleteByUserId(1L);
        inOrder.verify(fcmTokenRepository).deleteByUserId(1L);
        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getDisplayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(user.getProfileImageObjectKey()).isNull();
    }

    @Test
    @DisplayName("활성 그룹이 있으면 회원 탈퇴 시 그룹 탈퇴 흐름을 호출한다")
    void withdrawMe_callsGroupWithdrawFlowWhenActiveGroupExists() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        GroupMemberService groupMemberService = mock(GroupMemberService.class);
        GroupService groupService = mock(GroupService.class);
        FcmTokenRepository fcmTokenRepository = mock(FcmTokenRepository.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = new UserService(
                userRepository,
                socialLoginUserRepository,
                refreshTokenSessionService,
                jwtTokenProvider,
                new ImageReadUrlBuilder(new StorageProperties(TEST_IMAGE_BASE_URL)),
                uploadObjectKeyValidator(),
                groupMemberService,
                groupService,
                fcmTokenRepository
        );
        User user = User.createNew("카카오닉네임", "profile-images/1/profile.png");
        ReflectionTestUtils.setField(user, "id", 1L);
        GroupMember groupMember = GroupMember.createMember(1L, 10L);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(groupMemberService.getActiveGroupMembers(1L)).thenReturn(List.of(groupMember));

        // when
        userService.withdrawMe(1L);

        // then
        verify(groupService).withdrawGroup(10L, 1L);
        verify(socialLoginUserRepository).deleteByUserId(1L);
        verify(refreshTokenSessionService).deleteByUserId(1L);
        verify(fcmTokenRepository).deleteByUserId(1L);
        assertThat(user.isWithdrawn()).isTrue();
    }

    @Test
    @DisplayName("탈퇴할 accessToken에 해당하는 유저가 없으면 예외를 던진다")
    void withdraw_throwsExceptionWhenUserDoesNotExist() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);
        String accessToken = jwtTokenProvider.createAccessToken(999L);

        when(userRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.withdraw(accessToken))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    private UserService userService(
            UserRepository userRepository,
            SocialLoginUserRepository socialLoginUserRepository,
            RefreshTokenSessionService refreshTokenSessionService,
            JwtTokenProvider jwtTokenProvider
    ) {
        return new UserService(
                userRepository,
                socialLoginUserRepository,
                refreshTokenSessionService,
                jwtTokenProvider,
                new ImageReadUrlBuilder(new StorageProperties(TEST_IMAGE_BASE_URL)),
                uploadObjectKeyValidator(),
                mock(GroupMemberService.class),
                mock(GroupService.class),
                mock(FcmTokenRepository.class)
        );
    }

    private UploadObjectKeyValidator uploadObjectKeyValidator() {
        return new UploadObjectKeyValidator(List.of(new ProfileImageUploadPurposePolicy()));
    }
}
