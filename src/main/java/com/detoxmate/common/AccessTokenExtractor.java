package com.detoxmate.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AccessTokenExtractor {

    private AccessTokenExtractor() {}

    public static String require(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header is required");
        }

        String bearerPrefix = "Bearer ";
        if (!authorizationHeader.startsWith(bearerPrefix)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is required");
        }

        return authorizationHeader.substring(bearerPrefix.length());
    }
}
