package com.detoxmate.feed.dto.response;

public record FeedDetailPokedUser(
        Long userId,
        String displayName,
        String profileImageUrl
) {
}
