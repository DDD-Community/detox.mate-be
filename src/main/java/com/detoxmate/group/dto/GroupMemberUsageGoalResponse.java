package com.detoxmate.group.dto;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;

import java.time.LocalDateTime;

public record GroupMemberUsageGoalResponse(
        Long id,
        UsageGoalTypeCode usageGoalType,
        Integer goalMinutes,
        LocalDateTime setAt
) {
}
