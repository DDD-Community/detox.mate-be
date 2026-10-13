package com.detoxmate.friend.dto;

import java.util.List;

public record FriendProfileResponse(
        Long userId,
        String displayName,
        String profileImageUrl,
        String userCode,
        FriendRelationshipStatus relationshipStatus,
        List<FriendPreview> friends
) {
    public record FriendPreview(String displayName, String profileImageUrl) {
    }
}
