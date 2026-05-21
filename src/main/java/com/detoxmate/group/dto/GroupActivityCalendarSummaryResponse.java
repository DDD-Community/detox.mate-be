package com.detoxmate.group.dto;

import java.time.LocalDate;

public record GroupActivityCalendarSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        int allCount,
        int halfCount,
        int resetCount
) {
}
