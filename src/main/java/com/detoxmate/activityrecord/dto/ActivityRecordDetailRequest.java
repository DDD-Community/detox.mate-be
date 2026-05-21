package com.detoxmate.activityrecord.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActivityRecordDetailRequest(
        @NotNull
        UsageGoalTypeCode usageGoalType,

        @NotNull
        @Min(0)
        Integer usedMinutes
) {
}
