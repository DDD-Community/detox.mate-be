package com.detoxmate.screentimeocr.dto;

import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;

import java.time.LocalDateTime;

public record ScreenTimeOcrErrorReportUpdateResponse(
        Long id,
        ScreenTimeOcrErrorReportStatus status,
        Integer ocrTotalUsedMinutes,
        Integer correctedTotalUsedMinutes,
        String adminNote,
        String resolvedBy,
        LocalDateTime resolvedAt
) {
}
