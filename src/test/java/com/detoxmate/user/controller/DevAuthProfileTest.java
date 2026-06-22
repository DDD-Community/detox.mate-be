package com.detoxmate.user.controller;

import com.detoxmate.user.service.AuthService;
import com.detoxmate.user.service.DevAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DevAuthProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(AuthService.class, () -> mock(AuthService.class))
            .withUserConfiguration(DevAuthService.class, DevAuthController.class);

    @Test
    @DisplayName("prod 프로필에서도 테스트 로그인 엔드포인트를 등록한다")
    void testLoginEndpoint_isRegistered_whenProdProfileIsActive() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasSingleBean(DevAuthService.class);
                    assertThat(context).hasSingleBean(DevAuthController.class);
                });
    }
}
