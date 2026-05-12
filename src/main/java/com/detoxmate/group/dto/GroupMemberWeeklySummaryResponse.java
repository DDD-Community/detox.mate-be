package com.detoxmate.group.dto;

import java.time.LocalDate;

public record GroupMemberWeeklySummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int averageUsedMinutes,
        Integer goalMinutes,
        Integer differenceMinutes,
        int certifiedDays,
        int achievedDays
) {
}
