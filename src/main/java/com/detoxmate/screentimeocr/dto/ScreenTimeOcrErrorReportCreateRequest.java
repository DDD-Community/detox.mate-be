package com.detoxmate.screentimeocr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ScreenTimeOcrErrorReportCreateRequest(
        Long activityRecordId,

        @NotNull
        Long groupChallengeParticipantId,

        @NotNull
        LocalDate recordDate,

        @NotBlank
        String imageObjectKey,

        @NotNull
        @Min(0)
        Integer ocrTotalUsedMinutes
) {
}
