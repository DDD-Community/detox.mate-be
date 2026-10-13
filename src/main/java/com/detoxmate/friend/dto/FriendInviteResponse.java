package com.detoxmate.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FriendInviteResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String userCode
) {
}
