package com.detoxmate.activityrecord.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityRecordCreateResponse(
        long id,
        LocalDateTime createdAt,
        Long groupChallengeParticipantId,
        String activityImageUrl,
        String reflectionText,
        List<ActivityRecordDetailResult> details,
        boolean allAchieved
) {
}
