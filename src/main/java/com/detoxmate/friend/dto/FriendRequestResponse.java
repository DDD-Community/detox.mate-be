package com.detoxmate.friend.dto;

import java.time.LocalDateTime;

public record FriendRequestResponse(
        Long requestId,
        FriendUserResponse user,
        LocalDateTime createdAt
) {
}
