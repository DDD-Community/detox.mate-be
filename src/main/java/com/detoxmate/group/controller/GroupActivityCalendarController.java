package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.group.dto.GroupActivityCalendarResponse;
import com.detoxmate.group.service.GroupActivityCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupActivityCalendarController {

    private final GroupActivityCalendarService groupActivityCalendarService;

    @GetMapping("/group-challenges/{groupChallengeId}/activity-calendar")
    public GroupActivityCalendarResponse getActivityCalendar(
            CurrentUser currentUser,
            @PathVariable Long groupChallengeId
    ) {
        return groupActivityCalendarService.getCalendar(groupChallengeId, currentUser.id());
    }
}
