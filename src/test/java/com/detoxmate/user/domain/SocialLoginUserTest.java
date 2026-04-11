package com.detoxmate.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SocialLoginUserTest {

    @Test
    void 소셜로그인_유저를_유저와_provider_providerUserId로_연결한다() {
        // given
        User user = User.createNew("kakao-nickname");

        // when
        SocialLoginUser socialLoginUser = SocialLoginUser.link(user, SocialProvider.KAKAO, "123456789");

        // then
        assertThat(socialLoginUser.getId()).isNull();
        assertThat(socialLoginUser.getUser()).isSameAs(user);
        assertThat(socialLoginUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(socialLoginUser.getProviderUserId()).isEqualTo("123456789");
        assertThat(socialLoginUser.getCreatedAt()).isNull();
        assertThat(socialLoginUser.getUpdatedAt()).isNull();
    }
}
