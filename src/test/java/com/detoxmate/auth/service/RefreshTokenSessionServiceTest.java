package com.detoxmate.auth.service;

import com.detoxmate.auth.RefreshTokenProvider;
import com.detoxmate.auth.domain.RefreshTokenSession;
import com.detoxmate.auth.repository.RefreshTokenSessionRepository;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenSessionServiceTest {

    @Test
    void refresh_token을_발급하면_hash로_세션을_저장하고_raw_token을_반환한다() {
        // given
        RefreshTokenSessionRepository refreshTokenSessionRepository = mock(RefreshTokenSessionRepository.class);
        RefreshTokenProvider refreshTokenProvider = mock(RefreshTokenProvider.class);
        RefreshTokenSessionService refreshTokenSessionService = new RefreshTokenSessionService(
                refreshTokenSessionRepository,
                refreshTokenProvider
        );
        User user = User.createNew("kakao-nickname");

        when(refreshTokenProvider.createRefreshToken()).thenReturn("raw-refresh-token");
        when(refreshTokenProvider.getRefreshTokenExpiresIn()).thenReturn(15552000L);

        // when
        String refreshToken = refreshTokenSessionService.issueRefreshToken(user);

        // then
        ArgumentCaptor<RefreshTokenSession> captor = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(refreshTokenSessionRepository).save(captor.capture());

        RefreshTokenSession savedSession = captor.getValue();
        assertThat(refreshToken).isEqualTo("raw-refresh-token");
        assertThat(savedSession.getUser()).isSameAs(user);
        assertThat(savedSession.getTokenHash()).isNotEqualTo("raw-refresh-token");
        assertThat(savedSession.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void 유효한_refresh_token이면_session을_반환한다() {
        // given
        RefreshTokenSessionRepository refreshTokenSessionRepository = mock(RefreshTokenSessionRepository.class);
        RefreshTokenProvider refreshTokenProvider = mock(RefreshTokenProvider.class);
        RefreshTokenSessionService refreshTokenSessionService = new RefreshTokenSessionService(
                refreshTokenSessionRepository,
                refreshTokenProvider
        );
        User user = User.createNew("kakao-nickname");
        RefreshTokenSession refreshTokenSession = RefreshTokenSession.issue(
                user,
                refreshTokenSessionService.hash("raw-refresh-token"),
                LocalDateTime.now().plusDays(1)
        );

        when(refreshTokenSessionRepository.findByTokenHash(refreshTokenSessionService.hash("raw-refresh-token")))
                .thenReturn(Optional.of(refreshTokenSession));

        // when
        RefreshTokenSession foundSession = refreshTokenSessionService.getValidSession("raw-refresh-token");

        // then
        assertThat(foundSession).isSameAs(refreshTokenSession);
    }

    @Test
    void 만료된_refresh_token이면_401_예외를_던진다() {
        // given
        RefreshTokenSessionRepository refreshTokenSessionRepository = mock(RefreshTokenSessionRepository.class);
        RefreshTokenProvider refreshTokenProvider = mock(RefreshTokenProvider.class);
        RefreshTokenSessionService refreshTokenSessionService = new RefreshTokenSessionService(
                refreshTokenSessionRepository,
                refreshTokenProvider
        );
        User user = User.createNew("kakao-nickname");
        RefreshTokenSession refreshTokenSession = RefreshTokenSession.issue(
                user,
                refreshTokenSessionService.hash("expired-refresh-token"),
                LocalDateTime.now().minusSeconds(1)
        );

        when(refreshTokenSessionRepository.findByTokenHash(refreshTokenSessionService.hash("expired-refresh-token")))
                .thenReturn(Optional.of(refreshTokenSession));

        // when & then
        assertThatThrownBy(() -> refreshTokenSessionService.getValidSession("expired-refresh-token"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
