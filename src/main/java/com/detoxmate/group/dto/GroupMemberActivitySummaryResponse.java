package com.detoxmate.group.dto;

import java.time.LocalDate;

public record GroupMemberActivitySummaryResponse(
        LocalDate firstCertifiedDate,
        int dayCount,
        int achievementRate
) {
}
