package com.detoxmate.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void userId를_넣으면_access_token을_발급하고_다시_userId를_읽을_수_있다() {
        // given
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider("this-is-a-very-long-secret-key-for-temp-auth", 3600L);

        // when
        String accessToken = jwtTokenProvider.createAccessToken(1L);
        Long userId = jwtTokenProvider.getUserId(accessToken);

        // then
        assertThat(accessToken).isNotBlank();
        assertThat(userId).isEqualTo(1L);
    }
}
