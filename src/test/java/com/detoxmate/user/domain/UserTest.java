package com.detoxmate.user.domain;

import org.junit.jupiter.api.Test;

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
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }
}
