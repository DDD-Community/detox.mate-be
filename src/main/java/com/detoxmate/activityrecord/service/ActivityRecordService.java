package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailResult;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;

    public ActivityRecordAchievementCheckResponse checkAchievement(Long userId, ActivityRecordAchievementCheckRequest request) {
        List<ActivityRecordDetailResult> results = request.details().stream().map(detail -> {
            UserUsageGoalTime result = userUsageGoalTimeRepository.findTopByUser_IdAndUsageGoalType_CodeOrderByCreatedAtDesc(userId, detail.usageGoalType()).orElseThrow();

            int usedMinutes = detail.usedMinutes();
            int goalMinutes = result.getGoalMinutes().intValue();
            boolean isAchieved = usedMinutes <= goalMinutes;
            return new ActivityRecordDetailResult(detail.usageGoalType(), usedMinutes, goalMinutes, isAchieved);
        }).toList();

        boolean allAchieved = results.stream().allMatch(ActivityRecordDetailResult::isAchieved);

        return new ActivityRecordAchievementCheckResponse(
                results,
                allAchieved
        );
    }

    public ActivityRecordCreateResponse create(Long userId, ActivityRecordCreateRequest request) {
        throw new UnsupportedOperationException("Activity record create is not implemented yet.");
    }
}
