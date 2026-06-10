package com.detoxmate.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kakao.auth")
public record KakaoAuthProperties(
        String adminKey
) {
}
