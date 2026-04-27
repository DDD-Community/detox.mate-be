package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateResponse;

public interface ActivityRecordService {

    ActivityRecordAchievementCheckResponse checkAchievement(Long userId, ActivityRecordAchievementCheckRequest request);

    ActivityRecordCreateResponse create(Long userId, ActivityRecordCreateRequest request);
}
