package com.detoxmate.user.dto;

import com.detoxmate.common.validation.NullOrNotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 10)
        @NullOrNotBlank
        String displayName,

        @Size(max = 1024)
        @NullOrNotBlank
        String profileImageObjectKey
) {
}
