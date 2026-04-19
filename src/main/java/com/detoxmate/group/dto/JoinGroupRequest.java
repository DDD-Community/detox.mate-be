package com.detoxmate.group.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinGroupRequest(
        @NotBlank(message = "inviteCode is required")
        String inviteCode
) {
}
