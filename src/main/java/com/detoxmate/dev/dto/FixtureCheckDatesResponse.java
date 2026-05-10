package com.detoxmate.dev.dto;

import java.time.LocalDate;

public record FixtureCheckDatesResponse(
        LocalDate allDay,
        LocalDate halfDay,
        LocalDate today
) {
}
