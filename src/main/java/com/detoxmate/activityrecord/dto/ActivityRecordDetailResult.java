package com.detoxmate.activityrecord.dto;

public record ActivityRecordDetailResult(
        UsageGoalTypeCode usageGoalType,
        int useMinutes,
        int goalMinutes,
        boolean isAchieved
) {
}
