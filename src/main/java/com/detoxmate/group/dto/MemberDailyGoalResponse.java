package com.detoxmate.group.dto;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;

import java.time.LocalDate;

public record MemberDailyGoalResponse(
        Long userUsageGoalTimeId,
        UsageGoalTypeCode usageGoalType,
        Integer goalMinutes,
        LocalDate effectiveDate
) {
}
