package com.detoxmate.group.dto;

import com.detoxmate.group.domain.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        @NotBlank
        @Size(max = Group.MAX_NAME_LENGTH)
        String name
) {
}
