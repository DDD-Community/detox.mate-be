package com.detoxmate.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void 새_유저를_생성하면_displayName이_설정된다() {
        // given
        User user = User.createNew("kakao-nickname");

        // then
        assertThat(user.getId()).isNull();
        assertThat(user.getDisplayName()).isEqualTo("kakao-nickname");
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }
}
