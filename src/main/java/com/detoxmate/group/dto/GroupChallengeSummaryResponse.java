package com.detoxmate.group.dto;

import java.time.LocalDateTime;

public record GroupChallengeSummaryResponse(
        Long id,
        int challengeNo,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
