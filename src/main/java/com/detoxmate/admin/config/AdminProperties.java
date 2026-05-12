package com.detoxmate.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        String reviewToken,
        String actorName
) {
    public String actorNameOrDefault() {
        if (actorName == null || actorName.isBlank()) {
            return "MVP_ADMIN";
        }
        return actorName;
    }
}
