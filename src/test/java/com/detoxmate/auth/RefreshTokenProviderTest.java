package com.detoxmate.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenProviderTest {

    @Test
    void refresh_token을_발급하고_만료시간을_읽을_수_있다() {
        // given
        RefreshTokenProvider refreshTokenProvider = new RefreshTokenProvider(15552000L);

        // when
        String refreshToken = refreshTokenProvider.createRefreshToken();

        // then
        assertThat(refreshToken).isNotBlank();
        assertThat(refreshTokenProvider.getRefreshTokenExpiresIn()).isEqualTo(15552000L);
    }
}
