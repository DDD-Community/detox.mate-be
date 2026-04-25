package com.detoxmate.activityrecord.dto;

import java.util.List;

public record ActivityRecordAchievementCheckResponse(
        List<ActivityRecordDetailResult> details,
        boolean allAchieved
) {
}
