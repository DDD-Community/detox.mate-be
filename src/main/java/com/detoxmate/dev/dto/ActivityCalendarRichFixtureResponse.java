package com.detoxmate.dev.dto;

import java.time.LocalDate;
import java.util.List;

public record ActivityCalendarRichFixtureResponse(
        String fixture,
        Long groupId,
        Long groupChallengeId,
        String inviteCode,
        LocalDate today,
        LocalDate firstVerificationDate,
        FixtureSummaryResponse summary,
        FixtureCheckDatesResponse checkDates,
        List<FixtureUserResponse> users
) {
}
