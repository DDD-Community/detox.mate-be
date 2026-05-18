package com.detoxmate.user.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {
    @Test
    void 새_유저를_생성하면_displayName과_profileImageObjectKey가_설정된다() {
        // given
        User user = User.createNew("kakao-nickname", "profile-images/1/profile.png");

        // then
        assertThat(user.getId()).isNull();
        assertThat(user.getDisplayName()).isEqualTo("kakao-nickname");
        assertThat(user.getProfileImageObjectKey()).isEqualTo("profile-images/1/profile.png");
        assertThat(user.isActive()).isTrue();
        assertThat(user.isWithdrawn()).isFalse();
        assertThat(user.isPushNotificationEnabled()).isTrue();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void 알림_수신_여부를_변경한다() {
        User user = User.createNew("kakao-nickname");

        user.updatePushNotificationEnabled(false);

        assertThat(user.isPushNotificationEnabled()).isFalse();
    }

    @Test
    void status가_null인_기존_유저는_active로_간주한다() {
        User user = User.createNew("legacy-user");
        ReflectionTestUtils.setField(user, "status", null);

        assertThat(user.isActive()).isTrue();
        assertThat(user.isWithdrawn()).isFalse();
    }

    @Test
    void 회원_탈퇴하면_상태를_WITHDRAWN으로_바꾸고_프로필을_익명화한다() {
        User user = User.createNew("kakao-nickname", "profile-images/1/profile.png");

        user.withdraw();

        assertThat(user.isActive()).isFalse();
        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getWithdrawnAt()).isNotNull();
        assertThat(user.getDisplayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(user.getProfileImageObjectKey()).isNull();
        assertThat(user.getPublicDisplayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(user.getPublicProfileImageObjectKey()).isNull();
    }
}
