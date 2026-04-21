package com.detoxmate.group.dto;

import com.detoxmate.group.domain.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "name is required")
        @Size(max = Group.MAX_NAME_LENGTH, message = "name must be at most 12 characters")
        String name
) {
}
