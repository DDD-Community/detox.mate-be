package com.detoxmate.friend.dto;

import java.time.LocalDateTime;

public record FriendReceivedRequestResponse(
        Long requestId,
        FriendListUserResponse user,
        LocalDateTime createdAt
) {
}
