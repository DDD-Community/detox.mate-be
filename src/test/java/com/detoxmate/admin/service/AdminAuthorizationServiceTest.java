package com.detoxmate.admin.service;

import com.detoxmate.admin.config.AdminProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAuthorizationServiceTest {

    @Test
    void 설정된_admin_token과_일치하면_admin_actor를_반환한다() {
        AdminAuthorizationService adminAuthorizationService = new AdminAuthorizationService(
                new AdminProperties("secret-token", "OCR_REVIEWER")
        );

        String adminActor = adminAuthorizationService.requireAdmin("secret-token");

        assertThat(adminActor).isEqualTo("OCR_REVIEWER");
    }

    @Test
    void 설정된_admin_token과_다르면_admin_API에_접근할_수_없다() {
        AdminAuthorizationService adminAuthorizationService = new AdminAuthorizationService(
                new AdminProperties("secret-token", "OCR_REVIEWER")
        );

        assertThatThrownBy(() -> adminAuthorizationService.requireAdmin("wrong-token"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void admin_token이_설정되지_않으면_admin_API를_열지_않는다() {
        AdminAuthorizationService adminAuthorizationService = new AdminAuthorizationService(
                new AdminProperties("", "OCR_REVIEWER")
        );

        assertThatThrownBy(() -> adminAuthorizationService.requireAdmin("secret-token"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
