package com.detoxmate.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.apple.auth")
public record AppleAuthProperties(
        String clientId,
        String teamId,
        String keyId,
        String privateKey
) {
}
