package com.detoxmate.screentimeocr.dto;

import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;

import java.time.LocalDateTime;

public record ScreenTimeOcrErrorReportCreateResponse(
        Long id,
        ScreenTimeOcrErrorReportStatus status,
        LocalDateTime createdAt
) {
}
