package com.detoxmate.group.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest(
        @NotBlank(message = "name is required")
        String name
) {
}
