package com.detoxmate.common.logging;

import java.util.List;
import java.util.Locale;

final class SensitiveLogKeywordMatcher {

    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "password",
            "token",
            "identitytoken",
            "accesstoken",
            "refreshtoken",
            "providertoken",
            "provideraccesstoken",
            "authorizationcode",
            "authorization",
            "secret",
            "nonce",
            "rawnonce",
            "code",
            "key",
            "credential",
            "cookie",
            "session"
    );

    private SensitiveLogKeywordMatcher() {
    }

    static boolean matches(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYWORDS.stream()
                .anyMatch(normalized::contains);
    }
}
