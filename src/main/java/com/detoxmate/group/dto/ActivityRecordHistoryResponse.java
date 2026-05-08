package com.detoxmate.group.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityRecordHistoryResponse(
        Long id,
        LocalDateTime submittedAt,
        String activityImageUrl,
        String reflectionText,
        boolean allAchieved,
        List<ActivityRecordDetailHistoryResponse> details,
        int reactionCount,
        int commentCount
) {
}
