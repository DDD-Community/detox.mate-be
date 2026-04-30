package com.detoxmate.activityrecord.dto;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;

import java.time.LocalDateTime;

public record UsageGoalTimeResponse(
        Long id,
        UsageGoalTypeCode usageGoalType,
        Integer goalMinutes,
        LocalDateTime createdAt
) {
    public static UsageGoalTimeResponse from(UserUsageGoalTime goalTime) {
        return new UsageGoalTimeResponse(
                goalTime.getId(),
                goalTime.getUsageGoalType().getCode(),
                goalTime.getGoalMinutes(),
                goalTime.getCreatedAt()
        );
    }
}
