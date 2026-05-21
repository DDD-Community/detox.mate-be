package com.detoxmate.group.dto;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ActivityRecordDetailHistoryResponse(
        UsageGoalTypeCode usageGoalType,
        Integer usedMinutes,
        Integer goalMinutes,
        @JsonProperty("isAchieved") boolean isAchieved
) {
}
