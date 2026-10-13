package com.detoxmate.friend.dto;

public record FriendSearchResponse(
        Long userId,
        String displayName,
        String profileImageUrl,
        FriendRelationshipStatus relationshipStatus,
        Long requestId,
        long mutualFriendCount,
        String mutualFriendPreviewName
) {
}
