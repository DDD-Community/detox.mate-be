package com.detoxmate.group.dto;

import java.time.LocalDate;

public record GroupActivityCalendarResponse(
        Long groupId,
        int streakDays,
        GroupActivityCalendarSummaryResponse summary
) {
}
