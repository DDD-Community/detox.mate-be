package com.detoxmate.dev.dto;

public record FixtureSummaryResponse(
        int allCount,
        int halfCount,
        int resetCount,
        int streakDays
) {
}
