package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.group.dto.GroupActivityCalendarResponse;
import com.detoxmate.group.dto.GroupActivityFeedResponse;
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

    @GetMapping("/groups/{groupId}/activity-feed/days/{date}")
    public GroupActivityFeedResponse getActivityFeed(
            CurrentUser currentUser,
            @PathVariable Long groupId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return groupActivityCalendarService.getActivityFeed(groupId, date, currentUser.id());
    }

    @GetMapping("/groups/{groupId}/activity-feed/days/{date}/members/{groupMemberId}")
    public GroupActivityFeedResponse.MemberResponse getActivityFeedMember(
            CurrentUser currentUser,
            @PathVariable Long groupId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long groupMemberId
    ) {
        return groupActivityCalendarService.getActivityFeedMember(groupId, date, groupMemberId, currentUser.id());
    }
}
