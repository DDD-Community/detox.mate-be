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
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
    private static final String TEST_IMAGE_BASE_URL = "https://media.detoxmate.co.kr";

    @Test
    void accessToken으로_현재_유저를_조회하여_프로필_응답을_반환한다() {
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
    void accessToken에_해당하는_유저가_없으면_예외를_던진다() {
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
    void 탈퇴한_유저의_accessToken으로_조회하면_401_예외를_던진다() {
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);
        String accessToken = jwtTokenProvider.createAccessToken(1L);
        User user = User.createNew("카카오닉네임", "profile-images/1/profile.png");
        user.withdraw();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getMe(accessToken))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    @Test
    void displayName과_profileImageObjectKey를_함께_변경한다() {
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);
        User user = User.createNew("카카오닉네임", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MyProfileResponse response = userService.updateMe(
                1L,
                new UpdateMyProfileRequest("의진", "profile-images/1/updated.png")
        );

        assertThat(user.getDisplayName()).isEqualTo("의진");
        assertThat(user.getProfileImageObjectKey()).isEqualTo("profile-images/1/updated.png");
        assertThat(response.profileImageUrl()).isEqualTo(TEST_IMAGE_BASE_URL + "/profile-images/1/updated.png");
    }

    @Test
    void 다른_사용자_prefix의_profileImageObjectKey는_거부한다() {
        UserRepository userRepository = mock(UserRepository.class);
        SocialLoginUserRepository socialLoginUserRepository = mock(SocialLoginUserRepository.class);
        RefreshTokenSessionService refreshTokenSessionService = mock(RefreshTokenSessionService.class);
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRES_IN);
        UserService userService = userService(userRepository, socialLoginUserRepository, refreshTokenSessionService, jwtTokenProvider);
        User user = User.createNew("카카오닉네임", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateMe(
                1L,
                new UpdateMyProfileRequest(null, "profile-images/2/updated.png")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void accessToken으로_현재_유저를_조회하여_회원_탈퇴를_수행한다() {
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
    void 활성_그룹이_있으면_회원_탈퇴_시_그룹_탈퇴_흐름을_호출한다() {
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

        userService.withdrawMe(1L);

        verify(groupService).withdrawGroup(10L, 1L);
        verify(socialLoginUserRepository).deleteByUserId(1L);
        verify(refreshTokenSessionService).deleteByUserId(1L);
        verify(fcmTokenRepository).deleteByUserId(1L);
        assertThat(user.isWithdrawn()).isTrue();
    }

    @Test
    void 탈퇴할_accessToken에_해당하는_유저가_없으면_예외를_던진다() {
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
