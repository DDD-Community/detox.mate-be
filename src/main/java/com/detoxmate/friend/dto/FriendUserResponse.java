package com.detoxmate.friend.dto;

public record FriendUserResponse(
        Long userId,
        String displayName,
        String profileImageUrl,
        FriendRelationshipStatus relationshipStatus,
        Long requestId
) {
}
