package com.detoxmate.activityrecord.dto;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;

import java.time.LocalDateTime;

public record CurrentUsageGoalTimeResponse(
        Long id,
        UsageGoalTypeCode usageGoalType,
        Integer goalMinutes,
        LocalDateTime createdAt
) {
    public static CurrentUsageGoalTimeResponse from(UserUsageGoalTime goalTime) {
        return new CurrentUsageGoalTimeResponse(
                goalTime.getId(),
                goalTime.getUsageGoalType().getCode(),
                goalTime.getGoalMinutes(),
                goalTime.getCreatedAt()
        );
    }
}
