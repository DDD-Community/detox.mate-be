package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailResult;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {

    private static final String GOAL_TIME_NOT_FOUND_MESSAGE = "사용 가능한 목표 시간이 없습니다.";

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;

    public ActivityRecordAchievementCheckResponse checkAchievement(Long userId, ActivityRecordAchievementCheckRequest request) {
        List<ActivityRecordDetailRequest> details = request.details();
        List<UsageGoalTypeCode> requestedTypes = requestedTypes(details);

        LatestGoalTimes latestGoalTimes = LatestGoalTimes.from(
                userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeIn(userId, requestedTypes)
        );
        List<ActivityRecordDetailResult> results = toDetailResults(details, latestGoalTimes);

        validateGoalTimesFound(results);
        return toAchievementCheckResponse(results);
    }

    public ActivityRecordCreateResponse create(Long userId, ActivityRecordCreateRequest request) {
        throw new UnsupportedOperationException("Activity record create is not implemented yet.");
    }

    private List<UsageGoalTypeCode> requestedTypes(List<ActivityRecordDetailRequest> details) {
        return details.stream()
                .map(ActivityRecordDetailRequest::usageGoalType)
                .distinct()
                .toList();
    }

    private List<ActivityRecordDetailResult> toDetailResults(
            List<ActivityRecordDetailRequest> details,
            LatestGoalTimes latestGoalTimes
    ) {
        return details.stream()
                .map(detail -> latestGoalTimes.findBy(detail.usageGoalType())
                        .map(goalTime -> toDetailResult(detail, goalTime)))
                .flatMap(Optional::stream)
                .toList();
    }

    private ActivityRecordDetailResult toDetailResult(ActivityRecordDetailRequest detail, UserUsageGoalTime goalTime) {
        int usedMinutes = detail.usedMinutes();
        int goalMinutes = goalTime.getGoalMinutes();
        boolean isAchieved = usedMinutes <= goalMinutes;

        return new ActivityRecordDetailResult(
                detail.usageGoalType(),
                usedMinutes,
                goalMinutes,
                isAchieved
        );
    }

    private void validateGoalTimesFound(List<ActivityRecordDetailResult> results) {
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GOAL_TIME_NOT_FOUND_MESSAGE);
        }
    }

    private ActivityRecordAchievementCheckResponse toAchievementCheckResponse(List<ActivityRecordDetailResult> results) {
        boolean allAchieved = results.stream().allMatch(ActivityRecordDetailResult::isAchieved);
        return new ActivityRecordAchievementCheckResponse(results, allAchieved);
    }
}
