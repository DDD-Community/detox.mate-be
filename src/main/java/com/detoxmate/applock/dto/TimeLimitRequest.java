package com.detoxmate.applock.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimeLimitRequest(
        @NotNull @Min(0) @Max(1440) Integer totalLockMinutes
) {
}
