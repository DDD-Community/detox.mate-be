package com.detoxmate.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 30)
        @Pattern(regexp = ".*\\S.*")
        String displayName,

        @Size(max = 1024)
        @Pattern(regexp = ".*\\S.*")
        String profileImageObjectKey
) {
}
