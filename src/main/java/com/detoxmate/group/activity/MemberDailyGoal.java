package com.detoxmate.group.activity;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberDailyGoal(
        Long id,
        Long userId,
        UsageGoalTypeCode usageGoalType,
        int goalMinutes,
        LocalDate effectiveDate,
        LocalDateTime setAt
) {
}
