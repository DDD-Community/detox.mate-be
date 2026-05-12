package com.detoxmate.screentimeocr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScreenTimeOcrErrorReportUpdateRequest(
        @NotNull
        ScreenTimeOcrErrorReportUpdateAction action,

        @Min(0)
        Integer correctedTotalUsedMinutes,

        String adminNote
) {
}
