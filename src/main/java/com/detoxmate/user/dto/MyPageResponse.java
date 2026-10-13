package com.detoxmate.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MyPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String displayName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String userCode,
        @Schema(type = "string", types = {"string", "null"}, nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        String profileImageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean pushNotificationEnabled
) {
    public static MyPageResponse from(MyProfileResponse profile) {
        return new MyPageResponse(profile.id(), profile.displayName(), profile.userCode(),
                profile.profileImageUrl(), profile.pushNotificationEnabled());
    }
}
