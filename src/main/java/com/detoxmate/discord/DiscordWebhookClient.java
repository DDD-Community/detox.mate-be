package com.detoxmate.discord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@Slf4j
public class DiscordWebhookClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    public DiscordWebhookClient() {
        this(RestClient.builder()
                .requestFactory(requestFactory())
                .build());
    }

    DiscordWebhookClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void send(String webhookUrl, DiscordWebhookMessage message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Discord webhook URL is not configured. Skip notification.");
            return;
        }

        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(message)
                .retrieve()
                .toBodilessEntity();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }
}
