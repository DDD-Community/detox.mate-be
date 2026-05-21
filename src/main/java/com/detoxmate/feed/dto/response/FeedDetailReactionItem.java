package com.detoxmate.feed.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedDetailReactionItem(
        String reactionBody,
        Long userId,
        String displayName,
        String profileImageUrl,
        @JsonProperty("isUserWithdrawn")
        boolean userWithdrawn
) {
}
