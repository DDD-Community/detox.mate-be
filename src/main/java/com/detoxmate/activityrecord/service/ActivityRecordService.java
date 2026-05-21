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
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.notification.event.CertificationCreatedEvent;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String GOAL_TIME_NOT_FOUND_MESSAGE = "사용 가능한 목표 시간이 없습니다.";
    private static final String REFLECTION_TEXT_REQUIRED_MESSAGE = "미달성인 경우 reflectionText 는 필수입니다.";
    private static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    private static final String GROUP_CHALLENGE_PARTICIPANT_NOT_FOUND_MESSAGE = "그룹 챌린지 참여를 찾을 수 없습니다.";
    private static final String GROUP_CHALLENGE_PARTICIPANT_ACCESS_DENIED_MESSAGE = "요청한 그룹 챌린지 참여에 인증할 수 없습니다.";

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final UserRepository userRepository;
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final ChallengeRecordService challengeRecordService;
    private final Clock clock;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public ActivityRecordAchievementCheckResponse checkAchievement(
            Long userId,
            ActivityRecordAchievementCheckRequest request
    ) {
        LocalDate recordDate = today();
        List<ActivityRecordDetailRequest> details = request.details();
        List<UsageGoalTypeCode> requestedTypes = requestedTypes(details);

        LatestGoalTimes latestGoalTimes = effectiveGoalTimes(userId, requestedTypes, recordDate);
        List<ActivityRecordDetailResult> results = toDetailResults(details, latestGoalTimes);

        validateGoalTimesFound(results);
        return toAchievementCheckResponse(results);
    }

    @Transactional
    public ActivityRecordCreateResponse create(Long userId, ActivityRecordCreateRequest request) {
        LocalDate recordDate = today();
        List<ActivityRecordDetailRequest> details = request.details();
        List<UsageGoalTypeCode> requestedTypes = requestedTypes(details);

        LatestGoalTimes latestGoalTimes = effectiveGoalTimes(userId, requestedTypes, recordDate);
        List<ActivityRecordDetailResult> results = toDetailResults(details, latestGoalTimes);

        validateGoalTimesFound(results);
        validateReflectionText(request.reflectionText(), results);

        GroupChallengeParticipant groupChallengeParticipant = findGroupChallengeParticipant(request.groupChallengeParticipantId());
        validateGroupChallengeParticipantAccess(userId, groupChallengeParticipant.getId());
        User user = findUser(userId);

        ActivityRecord activityRecord = toActivityRecord(
                user,
                groupChallengeParticipant,
                request,
                results,
                latestGoalTimes
        );

        ActivityRecord savedActivityRecord = activityRecordRepository.save(activityRecord);
        ChallengeRecord challengeRecord = certifyChallengeRecord(savedActivityRecord, groupChallengeParticipant, results, recordDate);

        eventPublisher.publishEvent(new CertificationCreatedEvent(
                challengeRecord.getId(),
                userId
        ));
        return toCreateResponse(savedActivityRecord, results);
    }

    private LatestGoalTimes effectiveGoalTimes(
            Long userId,
            List<UsageGoalTypeCode> requestedTypes,
            LocalDate recordDate
    ) {
        LocalDateTime exclusiveUpperBound = recordDate.atStartOfDay();
        return LatestGoalTimes.from(
                userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeInAndCreatedAtBefore(
                        userId,
                        requestedTypes,
                        exclusiveUpperBound
                )
        );
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

    private void validateGroupChallengeParticipantAccess(Long userId, Long groupChallengeParticipantId) {
        if (!groupChallengeParticipantRepository.existsActiveByIdAndUserId(groupChallengeParticipantId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    GROUP_CHALLENGE_PARTICIPANT_ACCESS_DENIED_MESSAGE
            );
        }
    }

    private ActivityRecordAchievementCheckResponse toAchievementCheckResponse(List<ActivityRecordDetailResult> results) {
        boolean allAchieved = results.stream().allMatch(ActivityRecordDetailResult::isAchieved);
        return new ActivityRecordAchievementCheckResponse(results, allAchieved);
    }

    private ChallengeRecord certifyChallengeRecord(
            ActivityRecord savedActivityRecord,
            GroupChallengeParticipant groupChallengeParticipant,
            List<ActivityRecordDetailResult> results,
            LocalDate recordDate
    ) {
        ChallengeRecord challengeRecord = challengeRecordService.create(
                groupChallengeParticipant.getGroupChallengeId(),
                groupChallengeParticipant.getId(),
                recordDate
        );

        challengeRecordService.certify(
                challengeRecord.getId(),
                savedActivityRecord.getId(),
                groupChallengeParticipant.getId(),
                certificationResult(results)
        );

        return challengeRecord;
    }

    private ChallengeRecordCertificationResult certificationResult(List<ActivityRecordDetailResult> results) {
        if (allAchieved(results)) {
            return ChallengeRecordCertificationResult.SUCCESS;
        }
        return ChallengeRecordCertificationResult.FAIL;
    }

    private ActivityRecordCreateResponse toCreateResponse(
            ActivityRecord savedActivityRecord,
            List<ActivityRecordDetailResult> results
    ) {
        return new ActivityRecordCreateResponse(
                savedActivityRecord.getId(),
                savedActivityRecord.getCreatedAt(),
                savedActivityRecord.getGroupChallengeParticipant().getId(),
                imageReadUrlBuilder.build(savedActivityRecord.getActivityImageObjectKey()),
                savedActivityRecord.getReflectionText(),
                results,
                allAchieved(results)
        );
    }

    private boolean allAchieved(List<ActivityRecordDetailResult> results) {
        return results.stream().allMatch(ActivityRecordDetailResult::isAchieved);
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(KST));
    }
}
