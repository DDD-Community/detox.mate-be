package com.detoxmate.feed.dto.response;


public record FeedDetailReactionItem(
        String reactionBody,
        Long userId,
        String displayName,
        String profileImageUrl
) {
}
