package com.detoxmate.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import static com.detoxmate.user.domain.SocialProvider.TEST;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DevAuthServiceTest {

    @Test
    @DisplayName("legacy 테스트 유저 키이면 기존 표시 이름으로 로그인한다")
    void testLogin_legacyTestUserKey_logsInWithLegacyDisplayName() {
        AuthService authService = mock(AuthService.class);
        DevAuthService devAuthService = new DevAuthService(authService);

        devAuthService.testLogin("front-a");

        verify(authService).loginWithSocialUser(TEST, "front-a", "프론트 테스트 A", null);
    }

    @Test
    @DisplayName("test1 테스트 유저 키이면 계산된 표시 이름으로 로그인한다")
    void testLogin_dynamicFirstTestUserKey_logsInWithCalculatedDisplayName() {
        AuthService authService = mock(AuthService.class);
        DevAuthService devAuthService = new DevAuthService(authService);

        devAuthService.testLogin("test1");

        verify(authService).loginWithSocialUser(TEST, "test1", "테스트 유저 1", null);
    }

    @Test
    @DisplayName("test100 테스트 유저 키이면 계산된 표시 이름으로 로그인한다")
    void testLogin_dynamicLastTestUserKey_logsInWithCalculatedDisplayName() {
        AuthService authService = mock(AuthService.class);
        DevAuthService devAuthService = new DevAuthService(authService);

        devAuthService.testLogin("test100");

        verify(authService).loginWithSocialUser(TEST, "test100", "테스트 유저 100", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test0", "test101", "test001", "test+1", "Test1", "front-d", "unknown"})
    @DisplayName("허용되지 않은 테스트 유저 키이면 400 예외를 던진다")
    void testLogin_unsupportedTestUserKey_throwsBadRequest(String testUserKey) {
        AuthService authService = mock(AuthService.class);
        DevAuthService devAuthService = new DevAuthService(authService);

        assertThatThrownBy(() -> devAuthService.testLogin(testUserKey))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(400);
        verifyNoInteractions(authService);
    }
}
