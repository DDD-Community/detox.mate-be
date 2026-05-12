package com.detoxmate.admin.service;

import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private static final String ADMIN_ACCESS_DENIED_MESSAGE = "admin 권한이 필요합니다.";

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void requireAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ADMIN_ACCESS_DENIED_MESSAGE));

        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ADMIN_ACCESS_DENIED_MESSAGE);
        }
    }
}
