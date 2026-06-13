package com.detoxmate.firstscreentime.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FirstScreenTimeCreateRequest(
        @NotNull
        Long groupChallengeParticipantId,

        @NotNull
        @Min(0)
        Integer screenTimeMinutes,

        @NotNull
        LocalDate recordDate
) {
}
