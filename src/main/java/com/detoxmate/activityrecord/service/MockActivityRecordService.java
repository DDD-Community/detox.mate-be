package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailResult;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MockActivityRecordService implements ActivityRecordService {

    private static final Map<UsageGoalTypeCode, Integer> GOAL_MINUTES = new EnumMap<>(UsageGoalTypeCode.class);

    static {
        GOAL_MINUTES.put(UsageGoalTypeCode.TOTAL_USAGE, 60);
        GOAL_MINUTES.put(UsageGoalTypeCode.INSTAGRAM, 30);
        GOAL_MINUTES.put(UsageGoalTypeCode.YOUTUBE, 30);
    }

    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public ActivityRecordAchievementCheckResponse checkAchievement(Long userId, ActivityRecordAchievementCheckRequest request) {
        validateDuplicateUsageGoalType(request.details());
        List<ActivityRecordDetailResult> results = toResults(request.details());
        return new ActivityRecordAchievementCheckResponse(results, allAchieved(results));
    }

    @Override
    public ActivityRecordCreateResponse create(Long userId, ActivityRecordCreateRequest request) {
        validateDuplicateUsageGoalType(request.details());

        List<ActivityRecordDetailResult> results = toResults(request.details());
        boolean allAchieved = allAchieved(results);

        if (!allAchieved && isBlank(request.reflectionText())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "미달성인 경우 reflectionText 는 필수입니다.");
        }

        return new ActivityRecordCreateResponse(
                sequence.getAndIncrement(),
                LocalDateTime.now(),
                results,
                allAchieved
        );
    }

    private void validateDuplicateUsageGoalType(List<ActivityRecordDetailRequest> details) {
        Set<UsageGoalTypeCode> distinctTypes = details.stream()
                .map(ActivityRecordDetailRequest::usageGoalType)
                .collect(java.util.stream.Collectors.toSet());

        if (distinctTypes.size() != details.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 usageGoalType 는 허용되지 않습니다.");
        }
    }

    private List<ActivityRecordDetailResult> toResults(List<ActivityRecordDetailRequest> details) {
        return details.stream()
                .map(detail -> {
                    int goalMinutes = GOAL_MINUTES.get(detail.usageGoalType());
                    boolean isAchieved = detail.usedMinutes() <= goalMinutes;
                    return new ActivityRecordDetailResult(
                            detail.usageGoalType(),
                            detail.usedMinutes(),
                            goalMinutes,
                            isAchieved
                    );
                })
                .toList();
    }

    private boolean allAchieved(List<ActivityRecordDetailResult> details) {
        return details.stream().allMatch(ActivityRecordDetailResult::isAchieved);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
