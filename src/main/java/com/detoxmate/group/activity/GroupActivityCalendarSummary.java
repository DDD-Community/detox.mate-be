package com.detoxmate.group.activity;

import java.time.LocalDate;

public record GroupActivityCalendarSummary(
        LocalDate startDate,
        LocalDate endDate,
        int allCount,
        int halfCount,
        int resetCount
) {
}
