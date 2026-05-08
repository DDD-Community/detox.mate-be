package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.group.dto.GroupActivityCalendarHistoryResponse;
import com.detoxmate.group.dto.GroupActivityCalendarResponse;
import com.detoxmate.group.service.GroupActivityCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class GroupActivityCalendarController {

    private final GroupActivityCalendarService groupActivityCalendarService;

    @GetMapping("/groups/{groupId}/activity-calendar")
    public GroupActivityCalendarResponse getActivityCalendar(
            CurrentUser currentUser,
            @PathVariable Long groupId
    ) {
        return groupActivityCalendarService.getCalendar(groupId, currentUser.id());
    }

    @GetMapping("/groups/{groupId}/activity-calendar/days/{date}")
    public GroupActivityCalendarHistoryResponse getActivityCalendarHistory(
            CurrentUser currentUser,
            @PathVariable Long groupId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return groupActivityCalendarService.getCalendarHistory(groupId, date, currentUser.id());
    }
}
