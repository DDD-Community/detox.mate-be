package com.detoxmate.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 30)
        String displayName,

        @Size(max = 1024)
        String profileImageObjectKey
) {
}
