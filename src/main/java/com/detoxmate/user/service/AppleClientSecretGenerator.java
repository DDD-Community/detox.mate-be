package com.detoxmate.user.service;

import com.detoxmate.user.config.AppleAuthProperties;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final Duration CLIENT_SECRET_TTL = Duration.ofDays(180);

    private final AppleAuthProperties properties;
    private final Clock clock;

    @Autowired
    public AppleClientSecretGenerator(AppleAuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AppleClientSecretGenerator(AppleAuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String generate() {
        Instant now = clock.instant();
        try {
            return Jwts.builder()
                    .header()
                    .keyId(required(properties.keyId(), "Apple keyId is not configured"))
                    .and()
                    .issuer(required(properties.teamId(), "Apple teamId is not configured"))
                    .subject(required(properties.clientId(), "Apple clientId is not configured"))
                    .audience()
                    .add(APPLE_AUDIENCE)
                    .and()
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(CLIENT_SECRET_TTL)))
                    .signWith(privateKey(), Jwts.SIG.ES256)
                    .compact();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (GeneralSecurityException | RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Apple client secret generation failed", exception);
        }
    }

    private ECPrivateKey privateKey() throws GeneralSecurityException {
        String privateKey = required(properties.privateKey(), "Apple private key is not configured")
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(privateKey);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Apple private key is invalid", exception);
        }
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(keySpec);
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }

        return value;
    }
}
