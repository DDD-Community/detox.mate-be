package com.detoxmate.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FriendListUserResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String displayName,
        @Schema(type = "string", types = {"string", "null"}, nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        String profileImageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        FriendRelationshipStatus relationshipStatus,
        @Schema(type = "integer", format = "int64", types = {"integer", "null"}, nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        Long requestId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String userCode
) {
}
