package com.detoxmate.group.activity;

import java.time.LocalDate;

public record GroupDailyVerification(
        LocalDate date,
        CalendarDayStatus dayStatus,
        GroupDailyVerificationResult result,
        int activeMemberCount,
        int certifiedMemberCount,
        int requiredCount
) {
}
