package com.detoxmate.activityrecord.dto;

import java.util.List;

public record CurrentUsageGoalTimesResponse(
        List<CurrentUsageGoalTimeResponse> goals
) {
}
