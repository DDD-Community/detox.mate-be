package com.detoxmate.activityrecord.dto;

public record ActivityRecordDetailResult(
        UsageGoalTypeCode usageGoalType,
        int usedMinutes,
        int goalMinutes,
        boolean isAchieved
) {
}
