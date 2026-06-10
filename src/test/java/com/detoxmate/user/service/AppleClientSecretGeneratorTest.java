package com.detoxmate.user.service;

import com.detoxmate.user.config.AppleAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AppleClientSecretGeneratorTest {

    @Test
    void Apple_client_secret을_ES256_JWT로_생성한다() throws Exception {
        // given
        KeyPair keyPair = generateEcKeyPair();
        Instant now = Instant.parse("2026-05-27T00:00:00Z");
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
                new AppleAuthProperties(
                        "com.detoxmate.app",
                        "TEAM123456",
                        "KEY123456",
                        toPem(keyPair)
                ),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        // when
        String clientSecret = generator.generate();

        // then
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(clientSecret);
        assertThat(parsed.getHeader().getKeyId()).isEqualTo("KEY123456");
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(parsed.getPayload().getIssuer()).isEqualTo("TEAM123456");
        assertThat(parsed.getPayload().getSubject()).isEqualTo("com.detoxmate.app");
        assertThat(parsed.getPayload().getAudience()).containsExactly("https://appleid.apple.com");
        assertThat(parsed.getPayload().getIssuedAt()).isEqualTo(Date.from(now));
        assertThat(parsed.getPayload().getExpiration()).isEqualTo(Date.from(now.plusSeconds(180L * 24 * 60 * 60)));
    }

    private KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        return keyPairGenerator.generateKeyPair();
    }

    private String toPem(KeyPair keyPair) {
        String privateKey = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + privateKey + "\n-----END PRIVATE KEY-----";
    }
}
