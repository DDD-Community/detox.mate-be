package com.detoxmate.activityrecord.controller;

import com.detoxmate.activityrecord.dto.CurrentUsageGoalTimesResponse;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimesSetRequest;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimesSetResponse;
import com.detoxmate.activityrecord.service.UserUsageGoalTimeService;
import com.detoxmate.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserUsageGoalTimeController {

    private final UserUsageGoalTimeService userUsageGoalTimeService;

    @PostMapping("/me/usage-goal-times")
    @ResponseStatus(HttpStatus.CREATED)
    public UserUsageGoalTimesSetResponse setGoalTimes(
            CurrentUser currentUser,
            @Valid @RequestBody UserUsageGoalTimesSetRequest request
    ) {
        return userUsageGoalTimeService.setGoalTimes(currentUser.id(), request);
    }

    @GetMapping("/me/usage-goal-times/current")
    public CurrentUsageGoalTimesResponse getCurrentGoalTimes(CurrentUser currentUser) {
        return userUsageGoalTimeService.getCurrentGoalTimes(currentUser.id());
    }
}
