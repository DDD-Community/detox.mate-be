package com.detoxmate.group.dto;

import java.time.LocalDate;
import java.util.List;

public record GroupActivityCalendarHistoryResponse(
        Long groupId,
        LocalDate date,
        GroupDailyVerificationSummaryResponse dailySummary,
        List<CalendarHistoryMemberResponse> members
) {
}
