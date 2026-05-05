package com.detoxmate.group.dto;

public record MemberStatsResponse(
        MemberOverallStatsResponse overall,
        MemberRecent7DaysStatsResponse recent7Days
) {
}
