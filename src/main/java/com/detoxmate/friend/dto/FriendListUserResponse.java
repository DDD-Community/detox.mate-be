package com.detoxmate.friend.dto;

public record FriendListUserResponse(
        Long userId,
        String displayName,
        String profileImageUrl,
        FriendRelationshipStatus relationshipStatus,
        Long requestId,
        String email
) {
}
