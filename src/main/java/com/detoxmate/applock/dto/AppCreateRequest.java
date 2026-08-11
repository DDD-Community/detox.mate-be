package com.detoxmate.applock.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppCreateRequest(
        @NotNull
        Long userId,

        @NotBlank
        @Size(min = 1, max = 100)
        String appDisplayName,

        @NotNull
        @Min(0)
        @Max(1440)
        Integer dailyLimitMinutes
) {
}
