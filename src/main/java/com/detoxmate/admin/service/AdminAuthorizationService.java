package com.detoxmate.admin.service;

import com.detoxmate.admin.config.AdminProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private static final String ADMIN_ACCESS_DENIED_MESSAGE = "admin 권한이 필요합니다.";
    private static final String ADMIN_TOKEN_NOT_CONFIGURED_MESSAGE = "admin token 이 설정되지 않았습니다.";

    private final AdminProperties adminProperties;

    public void requireAdmin(String adminToken) {
        String configuredToken = adminProperties.reviewToken();
        if (configuredToken == null || configuredToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ADMIN_TOKEN_NOT_CONFIGURED_MESSAGE);
        }

        if (adminToken == null || adminToken.isBlank()
                || !MessageDigest.isEqual(
                        adminToken.getBytes(StandardCharsets.UTF_8),
                        configuredToken.getBytes(StandardCharsets.UTF_8)
                )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ADMIN_ACCESS_DENIED_MESSAGE);
        }
    }
}
