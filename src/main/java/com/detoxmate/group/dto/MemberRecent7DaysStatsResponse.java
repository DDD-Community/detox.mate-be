package com.detoxmate.group.dto;

import java.time.LocalDate;

public record MemberRecent7DaysStatsResponse(
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int submittedDays,
        int achievedDays
) {
}
