package com.detoxmate.activityrecord.dto;

import java.util.List;

public record UserUsageGoalTimesSetResponse(
        List<UsageGoalTimeResponse> goals
) {
}
