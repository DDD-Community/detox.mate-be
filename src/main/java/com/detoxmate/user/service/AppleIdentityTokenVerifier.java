package com.detoxmate.user.service;

import com.detoxmate.user.config.AppleAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AppleIdentityTokenVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_PUBLIC_KEYS_URI = "https://appleid.apple.com/auth/keys";

    private final JwtDecoder jwtDecoder;
    private final AppleAuthProperties properties;

    @Autowired
    public AppleIdentityTokenVerifier(AppleAuthProperties properties) {
        this(createAppleJwtDecoder(properties), properties);
    }

    AppleIdentityTokenVerifier(
            JwtDecoder jwtDecoder,
            AppleAuthProperties properties
    ) {
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
    }

    public String verify(String identityToken, String rawNonce) {
        requireConfiguredClientId();

        Jwt jwt = decode(identityToken);
        validateNonce(jwt, rawNonce);
        return extractProviderUserId(jwt);
    }

    private String extractProviderUserId(Jwt jwt) {
        String providerUserId = jwt.getSubject();
        if (providerUserId == null || providerUserId.isBlank()) {
            throw unauthorized("Apple identity token subject is missing");
        }

        return providerUserId;
    }

    private Jwt decode(String identityToken) {
        try {
            return jwtDecoder.decode(identityToken);
        } catch (JwtException | IllegalArgumentException exception) {
            throw unauthorized("Apple identity token is invalid", exception);
        }
    }

    private void validateNonce(Jwt jwt, String rawNonce) {
        String nonce = jwt.getClaimAsString("nonce");
        if (!NonceHasher.sha256Hex(rawNonce).equals(nonce)) {
            throw unauthorized("Apple identity token nonce is invalid");
        }
    }

    private String requireConfiguredClientId() {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Apple clientId is not configured");
        }

        return properties.clientId();
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private ResponseStatusException unauthorized(String message, Throwable cause) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, cause);
    }

    private static JwtDecoder createAppleJwtDecoder(AppleAuthProperties properties) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(APPLE_PUBLIC_KEYS_URI)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(APPLE_ISSUER),
                audienceValidator(properties)
        ));
        return jwtDecoder;
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(AppleAuthProperties properties) {
        return jwt -> {
            String clientId = properties.clientId();
            if (clientId != null && !clientId.isBlank() && jwt.getAudience().contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "Apple identity token audience is invalid",
                    null
            ));
        };
    }
}
