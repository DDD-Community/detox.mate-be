package com.detoxmate.activityrecord.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityRecordCreateResponse(
        long id,
        LocalDateTime createdAt,
        List<ActivityRecordDetailResult> details,
        boolean allAchieved
) {
}
