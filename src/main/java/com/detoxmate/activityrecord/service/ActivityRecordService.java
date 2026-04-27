package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailResult;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {

    private static final String GOAL_TIME_NOT_FOUND_MESSAGE = "사용 가능한 목표 시간이 없습니다.";
    private static final String REFLECTION_TEXT_REQUIRED_MESSAGE = "미달성인 경우 reflectionText 는 필수입니다.";
    private static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    private static final String GROUP_CHALLENGE_PARTICIPANT_NOT_FOUND_MESSAGE = "그룹 챌린지 참여를 찾을 수 없습니다.";

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final UserRepository userRepository;
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository;

    @Value("${app.storage.public-base-url:https://cdn.example.com}")
    private String storagePublicBaseUrl = "https://cdn.example.com";

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

    @Transactional
    public ActivityRecordCreateResponse create(Long userId, ActivityRecordCreateRequest request) {
        List<ActivityRecordDetailRequest> details = request.details();
        List<UsageGoalTypeCode> requestedTypes = requestedTypes(details);

        LatestGoalTimes latestGoalTimes = LatestGoalTimes.from(
                userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeIn(userId, requestedTypes)
        );
        List<ActivityRecordDetailResult> results = toDetailResults(details, latestGoalTimes);

        validateGoalTimesFound(results);
        validateReflectionText(request.reflectionText(), results);

        User user = findUser(userId);
        GroupChallengeParticipant groupChallengeParticipant = findGroupChallengeParticipant(request.groupChallengeParticipantId());

        ActivityRecord activityRecord = toActivityRecord(
                user,
                groupChallengeParticipant,
                request,
                results,
                latestGoalTimes
        );

        ActivityRecord savedActivityRecord = activityRecordRepository.save(activityRecord);
        return toCreateResponse(savedActivityRecord, results);
    }

    private List<UsageGoalTypeCode> requestedTypes(List<ActivityRecordDetailRequest> details) {
        return details.stream()
                .map(ActivityRecordDetailRequest::usageGoalType)
                .distinct()
                .toList();
    }

    private ActivityRecord toActivityRecord(
            User user,
            GroupChallengeParticipant groupChallengeParticipant,
            ActivityRecordCreateRequest request,
            List<ActivityRecordDetailResult> results,
            LatestGoalTimes latestGoalTimes
    ) {
        ActivityRecord activityRecord = ActivityRecord.create(
                user,
                groupChallengeParticipant,
                request.activityImageObjectKey(),
                request.reflectionText()
        );

        for (ActivityRecordDetailResult result : results) {
            UserUsageGoalTime goalTime = latestGoalTimes.findBy(result.usageGoalType())
                    .orElseThrow(() -> new IllegalStateException("latest goal time must exist for result type"));

            activityRecord.addDetail(goalTime, result.usedMinutes(), result.isAchieved());
        }

        return activityRecord;
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

    private void validateReflectionText(String reflectionText, List<ActivityRecordDetailResult> results) {
        boolean hasUnachievedDetail = results.stream().anyMatch(result -> !result.isAchieved());

        if (hasUnachievedDetail && (reflectionText == null || reflectionText.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, REFLECTION_TEXT_REQUIRED_MESSAGE);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE));
    }

    private GroupChallengeParticipant findGroupChallengeParticipant(Long groupChallengeParticipantId) {
        return groupChallengeParticipantRepository.findById(groupChallengeParticipantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        GROUP_CHALLENGE_PARTICIPANT_NOT_FOUND_MESSAGE
                ));
    }

    private ActivityRecordAchievementCheckResponse toAchievementCheckResponse(List<ActivityRecordDetailResult> results) {
        boolean allAchieved = results.stream().allMatch(ActivityRecordDetailResult::isAchieved);
        return new ActivityRecordAchievementCheckResponse(results, allAchieved);
    }

    private ActivityRecordCreateResponse toCreateResponse(
            ActivityRecord savedActivityRecord,
            List<ActivityRecordDetailResult> results
    ) {
        boolean allAchieved = results.stream().allMatch(ActivityRecordDetailResult::isAchieved);

        return new ActivityRecordCreateResponse(
                savedActivityRecord.getId(),
                savedActivityRecord.getCreatedAt(),
                savedActivityRecord.getGroupChallengeParticipant().getId(),
                toActivityImageUrl(savedActivityRecord.getActivityImageObjectKey()),
                savedActivityRecord.getReflectionText(),
                results,
                allAchieved
        );
    }

    private String toActivityImageUrl(String activityImageObjectKey) {
        if (activityImageObjectKey == null || activityImageObjectKey.isBlank()) {
            return null;
        }

        String normalizedBaseUrl = storagePublicBaseUrl.endsWith("/")
                ? storagePublicBaseUrl.substring(0, storagePublicBaseUrl.length() - 1)
                : storagePublicBaseUrl;

        return normalizedBaseUrl + "/" + activityImageObjectKey;
    }
}
