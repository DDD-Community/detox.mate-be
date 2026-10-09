package com.detoxmate.user.domain;

import com.detoxmate.support.UserFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {
    @Test
    @DisplayName("모든 신규 사용자 생성 방식은 명시적인 개인 코드를 저장한다")
    void createNew_preservesExplicitUserCode() {
        UserCode code = UserCode.parse("abcde");

        assertThat(User.createNew("user", code).getUserCode()).isEqualTo("ABCDE");
        assertThat(User.createNew("user", "profile.png", code).getUserCode()).isEqualTo("ABCDE");
        assertThat(User.createNew("user", "profile.png", null, code).getUserCode()).isEqualTo("ABCDE");
    }

    @Test
    @DisplayName("개인 코드 없이 새 사용자를 만들 수 없다")
    void createNew_rejectsMissingUserCode() {
        assertThatThrownBy(() -> User.createNew("user", (UserCode) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> User.createNew("user", null, (UserCode) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> User.createNew("user", null, null, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("프로필과 이메일 변경 및 탈퇴 후에도 개인 코드는 유지된다")
    void userCode_remainsStableAfterProfileChangesAndWithdrawal() {
        // given
        User user = User.createNew("user", new UserCode("ABCDE"));

        // when
        user.changeDisplayName("updated");
        user.changeProfileImageObjectKey("profile.png");
        user.registerEmailIfAbsent("USER@EXAMPLE.COM");
        user.withdraw();

        // then
        assertThat(user.getUserCode()).isEqualTo("ABCDE");
    }

    @Test
    void 새_유저를_생성하면_displayName과_profileImageObjectKey가_설정된다() {
        // given
        User user = UserFixtures.createUser("kakao-nickname", "profile-images/1/profile.png");

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
        User user = UserFixtures.createUser("kakao-nickname");

        user.updatePushNotificationEnabled(false);

        assertThat(user.isPushNotificationEnabled()).isFalse();
    }

    @Test
    void status가_null인_기존_유저는_active로_간주한다() {
        User user = UserFixtures.createUser("legacy-user");
        ReflectionTestUtils.setField(user, "status", null);

        assertThat(user.isActive()).isTrue();
        assertThat(user.isWithdrawn()).isFalse();
    }

    @Test
    void 회원_탈퇴하면_상태를_WITHDRAWN으로_바꾸고_프로필을_익명화한다() {
        User user = UserFixtures.createUser("kakao-nickname", "profile-images/1/profile.png", "USER@EXAMPLE.COM");

        user.withdraw();

        assertThat(user.isActive()).isFalse();
        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getWithdrawnAt()).isNotNull();
        assertThat(user.getDisplayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(user.getProfileImageObjectKey()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPublicDisplayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(user.getPublicProfileImageObjectKey()).isNull();
    }

    @Test
    void 새_유저의_이메일은_검색을_위해_정규화된다() {
        User user = UserFixtures.createUser("user", null, " User@Example.com ");

        assertThat(user.getEmail()).isEqualTo("user@example.com");
    }
}
