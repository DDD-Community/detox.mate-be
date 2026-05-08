package com.detoxmate.dev.controller;

import com.detoxmate.dev.dto.ActivityCalendarRichFixtureResponse;
import com.detoxmate.dev.service.ActivityCalendarRichFixtureService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "dev", "test"})
@RestController
@RequestMapping("/dev/fixtures")
@RequiredArgsConstructor
public class DevFixtureController {

    private final ActivityCalendarRichFixtureService activityCalendarRichFixtureService;

    @PostMapping("/activity-calendar-rich")
    public ActivityCalendarRichFixtureResponse seedActivityCalendarRich() {
        return activityCalendarRichFixtureService.seed();
    }
}
