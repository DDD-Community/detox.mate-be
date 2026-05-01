package com.detoxmate.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DevAuthServiceTest {

    @Test
    void 허용되지_않은_테스트_유저_키이면_400_예외를_던진다() {
        AuthService authService = mock(AuthService.class);
        DevAuthService devAuthService = new DevAuthService(authService);

        assertThatThrownBy(() -> devAuthService.testLogin("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(400);
        verifyNoInteractions(authService);
    }
}
