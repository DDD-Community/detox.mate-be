package com.detoxmate.notification.dto;

import com.detoxmate.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterFcmTokenRequest(
        @NotBlank String token,
        @NotNull DevicePlatform platform
) {
}
