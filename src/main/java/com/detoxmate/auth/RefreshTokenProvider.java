package com.detoxmate.auth;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class RefreshTokenProvider {

    private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    @Getter
    private final long refreshTokenExpiresIn;

    public RefreshTokenProvider(@Value("${auth.refresh-token.expires-in}") long refreshTokenExpiresIn) {
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    public String createRefreshToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
