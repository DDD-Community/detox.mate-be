package com.detoxmate.auth.service;

import com.detoxmate.auth.RefreshTokenProvider;
import com.detoxmate.auth.domain.RefreshTokenSession;
import com.detoxmate.auth.repository.RefreshTokenSessionRepository;
import com.detoxmate.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenSessionService {

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final RefreshTokenProvider refreshTokenProvider;

    public String issueRefreshToken(User user) {
        String refreshToken = refreshTokenProvider.createRefreshToken();
        String tokenHash = hash(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenProvider.getRefreshTokenExpiresIn());

        RefreshTokenSession refreshTokenSession = RefreshTokenSession.issue(user, tokenHash, expiresAt);
        refreshTokenSessionRepository.save(refreshTokenSession);

        return refreshToken;
    }

    public RefreshTokenSession getValidSession(String rawRefreshToken) {
        String hashedRefreshToken = hash(rawRefreshToken);
        RefreshTokenSession refreshTokenSession = refreshTokenSessionRepository.findByTokenHash(hashedRefreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (hasExpired(refreshTokenSession.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired refresh token");
        }

        if (refreshTokenSession.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Revoked refresh token");
        }

        return refreshTokenSession;
    }

    boolean hasExpired(LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();

        return expiresAt.isBefore(now);
    }

    String hash(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
