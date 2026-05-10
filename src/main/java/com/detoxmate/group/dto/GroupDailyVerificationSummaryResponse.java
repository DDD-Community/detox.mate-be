package com.detoxmate.group.dto;

import java.time.LocalDate;

public record GroupDailyVerificationSummaryResponse(
        LocalDate date,
        String dayStatus,
        String result,
        int activeMemberCount,
        int certifiedMemberCount,
        int requiredCount
) {
}
