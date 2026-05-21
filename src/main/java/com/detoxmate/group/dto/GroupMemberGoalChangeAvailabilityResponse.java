package com.detoxmate.group.dto;

import java.time.LocalDate;

public record GroupMemberGoalChangeAvailabilityResponse(
        boolean canChange,
        LocalDate nextChangeAvailableDate,
        int remainingDays
) {
}
