package com.detoxmate.discord;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscordWebhookMessage(
        String content,
        List<Embed> embeds
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Embed(
            String title,
            String description,
            Image image,
            List<Field> fields
    ) {
    }

    public record Image(String url) {
    }

    public record Field(
            String name,
            String value,
            boolean inline
    ) {
    }
}
