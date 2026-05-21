package com.detoxmate.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record RemoveFcmTokenRequest(
        @NotBlank String token
) {
}
