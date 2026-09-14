package com.detoxmate.friend.dto;

import java.time.LocalDateTime;

public record FriendResponse(
        Long friendshipId,
        FriendUserResponse user,
        LocalDateTime acceptedAt
) {
}
