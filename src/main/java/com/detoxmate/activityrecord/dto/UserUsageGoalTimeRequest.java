package com.detoxmate.activityrecord.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UserUsageGoalTimeRequest(
        @NotNull
        UsageGoalTypeCode usageGoalType,

        @NotNull
        @Min(0)
        @Max(1440)
        Integer goalMinutes
) {
}
