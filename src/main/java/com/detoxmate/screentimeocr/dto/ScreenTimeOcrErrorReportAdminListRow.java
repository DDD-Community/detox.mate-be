package com.detoxmate.screentimeocr.dto;

import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ScreenTimeOcrErrorReportAdminListRow(
        Long id,
        Long userId,
        String userDisplayName,
        Long activityRecordId,
        Long groupChallengeParticipantId,
        LocalDate recordDate,
        String imageObjectKey,
        Integer ocrTotalUsedMinutes,
        Integer correctedTotalUsedMinutes,
        ScreenTimeOcrErrorReportStatus status,
        String adminNote,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
