package com.detoxmate.feed.dto.response;


public record FeedDetailAuthorInfo(
        Long userId,
        String displayName,
        String profileImageUrl
) {
}