package com.detoxmate.feed.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedDetailPokedUser(
        Long userId,
        String displayName,
        String profileImageUrl,
        @JsonProperty("isUserWithdrawn")
        boolean userWithdrawn
) {
}
