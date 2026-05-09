package com.detoxmate.dev.controller;

import com.detoxmate.dev.dto.ActivityCalendarRichFixtureResponse;
import com.detoxmate.dev.service.ActivityCalendarSqlFixtureService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "dev", "test"})
@RestController
@RequestMapping("/dev/fixtures/activity-calendar-rich")
@RequiredArgsConstructor
public class ActivityCalendarSqlFixtureController {

    private final ActivityCalendarSqlFixtureService activityCalendarSqlFixtureService;

    @PostMapping("/reset")
    public ActivityCalendarRichFixtureResponse resetActivityCalendarRich() {
        return activityCalendarSqlFixtureService.reset();
    }
}
