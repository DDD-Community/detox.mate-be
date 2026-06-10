package com.detoxmate.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppleAuthProperties.class,
        KakaoAuthProperties.class,
        ProviderTokenEncryptionProperties.class
})
public class AppleAuthConfig {
}
