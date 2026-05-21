package com.detoxmate.screentimeocr.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.discord.ocr-report")
public record ScreenTimeOcrDiscordProperties(
        String webhookUrl,
        String apiBaseUrl
) {
    public boolean hasWebhookUrl() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    public String apiBaseUrlOrEnvReference() {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            return "$APP_PUBLIC_BASE_URL";
        }
        return trimTrailingSlash(apiBaseUrl);
    }

    private String trimTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
