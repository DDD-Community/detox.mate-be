package com.detoxmate.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record FriendResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long friendshipId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        FriendListUserResponse user,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime acceptedAt
) {
}
