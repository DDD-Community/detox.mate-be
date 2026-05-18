package com.detoxmate.user.service;

import com.detoxmate.user.config.AppleAuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleIdentityTokenVerifierTest {

    private static final String CLIENT_ID = "com.detoxmate.ios";
    private static final String RAW_NONCE = "apple-login-nonce-test";
    private static final String HASHED_NONCE = "a945dc4edee66edc03ce5d5b19f04cac8f01a5a034663d3be93b1201481121aa";
    private static final Instant ISSUED_AT = Instant.parse("2026-05-17T00:00:00Z");
    private static final Instant EXPIRES_AT = ISSUED_AT.plusSeconds(3600);

    @Test
    @DisplayName("유효한 Apple identity token이면 providerUserId를 반환한다")
    void verify_returnsProviderUserIdWhenIdentityTokenIsValid() {
        // given
        AppleIdentityTokenVerifier verifier = verifier(new StubJwtDecoder(jwt(
                Map.of(
                        "sub", "apple-sub",
                        "nonce", HASHED_NONCE
                )
        )));

        // when
        String providerUserId = verifier.verify("apple-id-token", RAW_NONCE);

        // then
        assertThat(providerUserId).isEqualTo("apple-sub");
    }

    @Test
    @DisplayName("Apple identity token 검증에 실패하면 401 예외를 던진다")
    void verify_throwsUnauthorizedWhenIdentityTokenIsRejected() {
        // given
        AppleIdentityTokenVerifier verifier = verifier(token -> {
            throw new JwtException("invalid apple id token");
        });

        // when & then
        assertUnauthorized(() -> verifier.verify("invalid-apple-id-token", RAW_NONCE));
    }

    @Test
    @DisplayName("nonce가 일치하지 않으면 401 예외를 던진다")
    void verify_throwsUnauthorizedWhenNonceDoesNotMatch() {
        // given
        AppleIdentityTokenVerifier verifier = verifier(new StubJwtDecoder(jwt(
                Map.of(
                        "sub", "apple-sub",
                        "nonce", HASHED_NONCE
                )
        )));

        // when & then
        assertUnauthorized(() -> verifier.verify("apple-id-token", "different-raw-nonce"));
    }

    @Test
    @DisplayName("Apple 사용자 식별자가 없으면 401 예외를 던진다")
    void verify_throwsUnauthorizedWhenSubjectIsMissing() {
        // given
        AppleIdentityTokenVerifier verifier = verifier(new StubJwtDecoder(jwt(
                Map.of("nonce", HASHED_NONCE)
        )));

        // when & then
        assertUnauthorized(() -> verifier.verify("apple-id-token", RAW_NONCE));
    }

    @Test
    @DisplayName("Apple 사용자 식별자가 공백이면 401 예외를 던진다")
    void verify_throwsUnauthorizedWhenSubjectIsBlank() {
        // given
        AppleIdentityTokenVerifier verifier = verifier(new StubJwtDecoder(jwt(
                Map.of(
                        "sub", "   ",
                        "nonce", HASHED_NONCE
                )
        )));

        // when & then
        assertUnauthorized(() -> verifier.verify("apple-id-token", RAW_NONCE));
    }

    @Test
    @DisplayName("Apple clientId 설정이 없으면 500 예외를 던진다")
    void verify_throwsInternalServerErrorWhenClientIdIsMissing() {
        // given
        AppleIdentityTokenVerifier verifier = new AppleIdentityTokenVerifier(
                new StubJwtDecoder(jwt(Map.of("sub", "apple-sub", "nonce", HASHED_NONCE))),
                new AppleAuthProperties("")
        );

        // when & then
        assertThatThrownBy(() -> verifier.verify("apple-id-token", RAW_NONCE))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private AppleIdentityTokenVerifier verifier(JwtDecoder jwtDecoder) {
        return new AppleIdentityTokenVerifier(
                jwtDecoder,
                new AppleAuthProperties(CLIENT_ID)
        );
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "apple-id-token",
                ISSUED_AT,
                EXPIRES_AT,
                Map.of("alg", "RS256"),
                claims
        );
    }

    private void assertUnauthorized(ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private record StubJwtDecoder(Jwt jwt) implements JwtDecoder {

        @Override
        public Jwt decode(String token) {
            return jwt;
        }
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call() throws Exception;
    }
}
