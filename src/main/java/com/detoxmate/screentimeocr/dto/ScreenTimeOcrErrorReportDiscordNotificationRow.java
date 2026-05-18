package com.detoxmate.screentimeocr.dto;

import java.time.LocalDate;

public record ScreenTimeOcrErrorReportDiscordNotificationRow(
        Long id,
        Long userId,
        String userDisplayName,
        Long activityRecordId,
        Long groupChallengeParticipantId,
        LocalDate recordDate,
        String imageObjectKey,
        Integer ocrTotalUsedMinutes
) {
}
