package com.detoxmate.friend.dto;

import jakarta.validation.constraints.NotNull;

public record CreateFriendRequest(
        @NotNull(message = "targetUserId is required")
        Long targetUserId
) {
}
