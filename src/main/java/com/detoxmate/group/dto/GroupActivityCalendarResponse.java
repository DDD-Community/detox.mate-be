package com.detoxmate.group.dto;

import java.time.LocalDate;

public record GroupActivityCalendarResponse(
        Long groupId,
        LocalDate firstVerificationDate,
        int streakDays,
        GroupActivityCalendarSummaryResponse summary
) {
}
