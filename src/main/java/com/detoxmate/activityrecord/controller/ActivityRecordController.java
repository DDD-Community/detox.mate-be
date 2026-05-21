package com.detoxmate.activityrecord.controller;

import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateResponse;
import com.detoxmate.activityrecord.service.ActivityRecordService;
import com.detoxmate.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActivityRecordController {

    private final ActivityRecordService activityRecordService;

    @PostMapping("/activity-records/achievement-check")
    public ActivityRecordAchievementCheckResponse checkAchievement(
            CurrentUser currentUser,
            @Valid @RequestBody ActivityRecordAchievementCheckRequest request
    ) {
        return activityRecordService.checkAchievement(currentUser.id(), request);
    }

    @PostMapping("/activity-records")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityRecordCreateResponse create(
            CurrentUser currentUser,
            @Valid @RequestBody ActivityRecordCreateRequest request
    ) {
        return activityRecordService.create(currentUser.id(), request);
    }
}
