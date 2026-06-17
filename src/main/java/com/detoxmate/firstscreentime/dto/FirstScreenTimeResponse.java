package com.detoxmate.firstscreentime.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FirstScreenTimeResponse(
        Long id,
        Long groupChallengeParticipantId,
        Integer screenTimeMinutes,
        LocalDate recordDate,
        LocalDateTime createdAt
) {
}
