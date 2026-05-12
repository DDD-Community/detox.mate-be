package com.detoxmate.admin.service;

import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAuthorizationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminAuthorizationService adminAuthorizationService = new AdminAuthorizationService(userRepository);

    @Test
    void ADMIN_role_유저는_admin_API에_접근할_수_있다() {
        User admin = user(99L);
        admin.grantAdminRole();
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));

        assertThatCode(() -> adminAuthorizationService.requireAdmin(99L))
                .doesNotThrowAnyException();
    }

    @Test
    void USER_role_유저는_admin_API에_접근할_수_없다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        assertThatThrownBy(() -> adminAuthorizationService.requireAdmin(1L))
                .isInstanceOf(ResponseStatusException.class);
    }

    private User user(Long id) {
        User user = User.createNew("tester");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
