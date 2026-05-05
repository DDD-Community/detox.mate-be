package com.detoxmate.group.dto;

import java.time.LocalDate;

public record MemberOverallStatsResponse(
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int achievedDays,
        int achievementRate
) {
}
