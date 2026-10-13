package com.detoxmate.friend.dto;

public record FriendInviteeResponse(
        Long userId,
        String displayName,
        String profileImageUrl,
        FriendRelationshipStatus relationshipStatus,
        Long requestId,
        long daysSinceStart,
        long targetSuccessCount
) {
}
