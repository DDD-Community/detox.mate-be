package com.detoxmate.activityrecord.dto;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;

import java.time.LocalDateTime;

public record CurrentUsageGoalTimeResponse(
        UsageGoalTypeCode usageGoalType,
        Integer goalMinutes,
        LocalDateTime setAt
) {
    public static CurrentUsageGoalTimeResponse from(UserUsageGoalTime goalTime) {
        return new CurrentUsageGoalTimeResponse(
                goalTime.getUsageGoalType().getCode(),
                goalTime.getGoalMinutes(),
                goalTime.getCreatedAt()
        );
    }
}
