package com.detoxmate.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.provider-token.encryption")
public record ProviderTokenEncryptionProperties(
        String key
) {
}
