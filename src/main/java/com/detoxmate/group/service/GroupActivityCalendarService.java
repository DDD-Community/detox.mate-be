package com.detoxmate.group.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.common.MemberActivityOrder;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.group.GroupActivityCalendarErrorCode;
import com.detoxmate.group.activity.CalendarDayStatus;
import com.detoxmate.group.activity.GroupActivityCalendarSummary;
import com.detoxmate.group.activity.GroupActivityParticipant;
import com.detoxmate.group.activity.GroupActivityVerificationPolicy;
import com.detoxmate.group.activity.GroupDailyVerification;
import com.detoxmate.group.activity.MemberDailyGoal;
import com.detoxmate.group.activity.MemberDailyStatus;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.dto.ActivityRecordDetailHistoryResponse;
import com.detoxmate.group.dto.ActivityRecordHistoryResponse;
import com.detoxmate.group.dto.CalendarHistoryMemberResponse;
import com.detoxmate.group.dto.GroupActivityCalendarHistoryResponse;
import com.detoxmate.group.dto.GroupActivityCalendarResponse;
import com.detoxmate.group.dto.GroupActivityCalendarSummaryResponse;
import com.detoxmate.group.dto.GroupActivityParticipantRow;
import com.detoxmate.group.dto.GroupDailyVerificationSummaryResponse;
import com.detoxmate.group.dto.MemberDailyGoalResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupActivityCalendarService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String ACTIVE_MEMBER_STATUS = "ACTIVE";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final GroupChallengeParticipantRepository participantRepository;
    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final ChallengeRecordService challengeRecordService;
    private final ActivityRecordRepository activityRecordRepository;
    private final ChallengeRecordStatusCountRepository statusCountRepository;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final GroupActivityVerificationPolicy verificationPolicy;
    private final Clock clock;

    public GroupActivityCalendarResponse getCalendar(Long groupId, Long currentUserId) {
        CalendarSource source = loadSource(groupId, currentUserId);
        LocalDate today = today();
        LocalDate firstVerificationDate = verificationPolicy.firstVerificationDate(
                source.participants(),
                source.goals(),
                today
        );
        Map<LocalDate, Set<Long>> certifiedParticipantIdsByDate = certifiedParticipantIdsByDate(
                source.challenge().getId(),
                firstVerificationDate,
                today
        );

        GroupActivityCalendarSummary summary = verificationPolicy.summarizeConfirmedDates(
                today,
                firstVerificationDate,
                source.participants(),
                source.goals(),
                certifiedParticipantIdsByDate
        );
        int streakDays = verificationPolicy.streakDays(
                today,
                firstVerificationDate,
                source.participants(),
                source.goals(),
                certifiedParticipantIdsByDate
        );

        return new GroupActivityCalendarResponse(
                groupId,
                firstVerificationDate,
                streakDays,
                toSummaryResponse(summary)
        );
    }

    @Transactional
    public GroupActivityCalendarHistoryResponse getCalendarHistory(Long groupId, LocalDate date, Long currentUserId) {
        CalendarSource source = loadSource(groupId, currentUserId);
        LocalDate today = today();
        LocalDate firstVerificationDate = verificationPolicy.firstVerificationDate(
                source.participants(),
                source.goals(),
                today
        );
        CalendarDayStatus dayStatus = dayStatus(date, today, firstVerificationDate);
        ensureTodayChallengeRecords(source, date, dayStatus);
        List<ChallengeRecord> challengeRecords = challengeRecordRepository.findAllByGroupChallengeDate(
                source.challenge().getId(),
                date
        );
        Set<Long> certifiedParticipantIds = certifiedParticipantIds(challengeRecords);

        return new GroupActivityCalendarHistoryResponse(
                groupId,
                date,
                toDailySummary(date, dayStatus, source.participants(), source.goals(), certifiedParticipantIds),
                toMembers(source, date, currentUserId, dayStatus, challengeRecords)
        );
    }

    private void ensureTodayChallengeRecords(CalendarSource source, LocalDate date, CalendarDayStatus dayStatus) {
        if (dayStatus != CalendarDayStatus.IN_PROGRESS) {
            return;
        }

        source.participants().stream()
                .filter(participant -> verificationPolicy.includedInGroupResult(participant, source.goals(), date))
                .forEach(participant -> challengeRecordService.create(
                        source.challenge().getId(),
                        participant.participantId(),
                        date
                ));
    }

    private CalendarSource loadSource(Long groupId, Long currentUserId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(GroupActivityCalendarErrorCode.GROUP_NOT_FOUND));
        validateGroupAccess(groupId, currentUserId);

        GroupChallenge challenge = groupChallengeRepository.findTopByGroupIdOrderByChallengeNoDesc(groupId)
                .orElseThrow(() -> new CustomException(GroupActivityCalendarErrorCode.GROUP_CHALLENGE_NOT_FOUND));
        List<GroupActivityParticipantRow> participantRows =
                participantRepository.findActivityParticipantRowsByGroupChallengeId(challenge.getId());
        List<GroupActivityParticipant> participants = participantRows.stream()
                .map(this::toParticipant)
                .toList();
        List<MemberDailyGoal> goals = memberDailyGoals(participantRows);

        return new CalendarSource(challenge, participantRows, participants, goals);
    }

    private void validateGroupAccess(Long groupId, Long currentUserId) {
        groupMemberRepository.findByUserIdAndGroupIdAndStatus(currentUserId, groupId, ACTIVE_MEMBER_STATUS)
                .orElseThrow(() -> new CustomException(GroupActivityCalendarErrorCode.GROUP_ACCESS_DENIED));
    }

    private List<MemberDailyGoal> memberDailyGoals(List<GroupActivityParticipantRow> participantRows) {
        Set<Long> userIds = participantRows.stream()
                .map(GroupActivityParticipantRow::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return List.of();
        }

        return userUsageGoalTimeRepository.findAllByUser_IdIn(userIds).stream()
                .map(this::toMemberDailyGoal)
                .toList();
    }

    private MemberDailyGoal toMemberDailyGoal(UserUsageGoalTime goalTime) {
        LocalDate effectiveDate = goalTime.getCreatedAt().toLocalDate().plusDays(1);

        return new MemberDailyGoal(
                goalTime.getId(),
                goalTime.getUser().getId(),
                goalTime.getUsageGoalType().getCode(),
                goalTime.getGoalMinutes(),
                effectiveDate,
                goalTime.getCreatedAt()
        );
    }

    private GroupActivityParticipant toParticipant(GroupActivityParticipantRow row) {
        return new GroupActivityParticipant(
                row.groupChallengeParticipantId(),
                row.groupMemberId(),
                row.userId(),
                row.memberStatus(),
                row.memberJoinedAt(),
                row.memberLeftAt(),
                row.participantStatus(),
                row.participantJoinedAt(),
                row.participantWithdrawnAt()
        );
    }

    private Map<LocalDate, Set<Long>> certifiedParticipantIdsByDate(
            Long groupChallengeId,
            LocalDate firstVerificationDate,
            LocalDate today
    ) {
        LocalDate endDate = today.minusDays(1);
        if (firstVerificationDate == null || endDate.isBefore(firstVerificationDate)) {
            return Map.of();
        }

        return challengeRecordRepository
                .findAllByGroupChallengeIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(
                        groupChallengeId,
                        firstVerificationDate,
                        endDate
                )
                .stream()
                .filter(ChallengeRecord::isCertified)
                .collect(Collectors.groupingBy(
                        ChallengeRecord::getRecordDate,
                        Collectors.mapping(ChallengeRecord::getGroupChallengeParticipantId, Collectors.toSet())
                ));
    }

    private Set<Long> certifiedParticipantIds(Collection<ChallengeRecord> challengeRecords) {
        return challengeRecords.stream()
                .filter(ChallengeRecord::isCertified)
                .map(ChallengeRecord::getGroupChallengeParticipantId)
                .collect(Collectors.toSet());
    }

    private GroupDailyVerificationSummaryResponse toDailySummary(
            LocalDate date,
            CalendarDayStatus dayStatus,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            Set<Long> certifiedParticipantIds
    ) {
        if (dayStatus == CalendarDayStatus.NOT_STARTED || dayStatus == CalendarDayStatus.FUTURE) {
            return new GroupDailyVerificationSummaryResponse(
                    date,
                    dayStatus.name(),
                    null,
                    0,
                    0,
                    0
            );
        }

        GroupDailyVerification verification = verificationPolicy.verifyConfirmedDate(
                date,
                participants,
                goals,
                certifiedParticipantIds
        );

        return new GroupDailyVerificationSummaryResponse(
                date,
                dayStatus.name(),
                dayStatus == CalendarDayStatus.CONFIRMED ? verification.result().name() : null,
                verification.activeMemberCount(),
                verification.certifiedMemberCount(),
                verification.requiredCount()
        );
    }

    private List<CalendarHistoryMemberResponse> toMembers(
            CalendarSource source,
            LocalDate date,
            Long currentUserId,
            CalendarDayStatus dayStatus,
            List<ChallengeRecord> challengeRecords
    ) {
        Map<Long, ChallengeRecord> challengeRecordByParticipantId = challengeRecords.stream()
                .collect(Collectors.toMap(
                        ChallengeRecord::getGroupChallengeParticipantId,
                        Function.identity(),
                        (first, second) -> first
                ));
        Map<Long, ActivityRecord> activityRecordById = activityRecordById(challengeRecords);
        Map<Long, ChallengeRecordStatusCount> statusCountByRecordId = statusCountByRecordId(challengeRecords);
        Map<Long, GroupActivityParticipant> participantById = source.participants().stream()
                .collect(Collectors.toMap(GroupActivityParticipant::participantId, Function.identity()));

        return source.participantRows().stream()
                .map(row -> toMemberResponse(
                        row,
                        participantById.get(row.groupChallengeParticipantId()),
                        source.goals(),
                        date,
                        dayStatus,
                        currentUserId,
                        challengeRecordByParticipantId.get(row.groupChallengeParticipantId()),
                        activityRecordById,
                        statusCountByRecordId
                ))
                .sorted(MemberActivityOrder.latestCertifiedThenDisplayName(
                        this::activitySubmittedAt,
                        CalendarHistoryMemberResponse::displayName
                ))
                .toList();
    }

    private CalendarHistoryMemberResponse toMemberResponse(
            GroupActivityParticipantRow row,
            GroupActivityParticipant participant,
            List<MemberDailyGoal> allGoals,
            LocalDate date,
            CalendarDayStatus dayStatus,
            Long currentUserId,
            ChallengeRecord challengeRecord,
            Map<Long, ActivityRecord> activityRecordById,
            Map<Long, ChallengeRecordStatusCount> statusCountByRecordId
    ) {
        boolean includedInGroupResult = (dayStatus == CalendarDayStatus.CONFIRMED
                || dayStatus == CalendarDayStatus.IN_PROGRESS)
                && participant != null
                && verificationPolicy.includedInGroupResult(participant, allGoals, date);
        ActivityRecordHistoryResponse activityRecord = toActivityRecordHistory(
                challengeRecord,
                activityRecordById,
                statusCountByRecordId
        );
        MemberDailyStatus dailyStatus = dailyStatus(includedInGroupResult, challengeRecord);

        return new CalendarHistoryMemberResponse(
                row.groupMemberId(),
                row.groupChallengeParticipantId(),
                row.userId(),
                row.displayName(),
                imageReadUrlBuilder.build(row.profileImageObjectKey()),
                Objects.equals(row.userId(), currentUserId),
                row.memberStatus(),
                row.participantStatus(),
                dailyStatus.name(),
                includedInGroupResult,
                toGoalResponses(verificationPolicy.effectiveGoals(row.userId(), allGoals, date)),
                activityRecord
        );
    }

    private Map<Long, ActivityRecord> activityRecordById(List<ChallengeRecord> challengeRecords) {
        List<Long> activityRecordIds = challengeRecords.stream()
                .filter(ChallengeRecord::isCertified)
                .map(ChallengeRecord::getActivityRecordId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (activityRecordIds.isEmpty()) {
            return Map.of();
        }

        return activityRecordRepository.findAllById(activityRecordIds).stream()
                .collect(Collectors.toMap(ActivityRecord::getId, Function.identity()));
    }

    private Map<Long, ChallengeRecordStatusCount> statusCountByRecordId(List<ChallengeRecord> challengeRecords) {
        List<Long> challengeRecordIds = challengeRecords.stream()
                .map(ChallengeRecord::getId)
                .filter(Objects::nonNull)
                .toList();

        if (challengeRecordIds.isEmpty()) {
            return Map.of();
        }

        return statusCountRepository.findAllByChallengeRecordIdIn(challengeRecordIds).stream()
                .collect(Collectors.toMap(ChallengeRecordStatusCount::getChallengeRecordId, Function.identity()));
    }

    private ActivityRecordHistoryResponse toActivityRecordHistory(
            ChallengeRecord challengeRecord,
            Map<Long, ActivityRecord> activityRecordById,
            Map<Long, ChallengeRecordStatusCount> statusCountByRecordId
    ) {
        if (challengeRecord == null || !challengeRecord.isCertified()) {
            return null;
        }

        ActivityRecord activityRecord = activityRecordById.get(challengeRecord.getActivityRecordId());
        if (activityRecord == null) {
            throw new CustomException(GroupActivityCalendarErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        List<ActivityRecordDetailHistoryResponse> details = activityRecord.getDetails().stream()
                .map(this::toActivityRecordDetail)
                .toList();
        ChallengeRecordStatusCount statusCount = statusCountByRecordId.get(challengeRecord.getId());

        return new ActivityRecordHistoryResponse(
                activityRecord.getId(),
                activityRecord.getCreatedAt(),
                imageReadUrlBuilder.build(activityRecord.getActivityImageObjectKey()),
                activityRecord.getReflectionText(),
                details.stream().allMatch(ActivityRecordDetailHistoryResponse::isAchieved),
                details,
                reactionCount(statusCount),
                afterCommentCount(statusCount)
        );
    }

    private ActivityRecordDetailHistoryResponse toActivityRecordDetail(ActivityRecordDetail detail) {
        return new ActivityRecordDetailHistoryResponse(
                detail.getUsageGoalType().getCode(),
                detail.getUseMinutes(),
                detail.getUserUsageGoalTime().getGoalMinutes(),
                detail.isAchieved()
        );
    }

    private List<MemberDailyGoalResponse> toGoalResponses(List<MemberDailyGoal> goals) {
        return goals.stream()
                .map(goal -> new MemberDailyGoalResponse(
                        goal.id(),
                        goal.usageGoalType(),
                        goal.goalMinutes(),
                        goal.effectiveDate()
                ))
                .toList();
    }

    private MemberDailyStatus dailyStatus(boolean includedInGroupResult, ChallengeRecord challengeRecord) {
        if (!includedInGroupResult) {
            return MemberDailyStatus.NOT_ACTIVE;
        }
        if (challengeRecord == null || !challengeRecord.isCertified()) {
            return MemberDailyStatus.NOT_CERTIFIED;
        }
        if (challengeRecord.isCertificationSucceeded()) {
            return MemberDailyStatus.GOAL_ACHIEVED;
        }
        return MemberDailyStatus.GOAL_FAILED;
    }

    private LocalDateTime activitySubmittedAt(CalendarHistoryMemberResponse member) {
        if (member.activityRecord() == null) {
            return null;
        }

        return member.activityRecord().submittedAt();
    }

    private GroupActivityCalendarSummaryResponse toSummaryResponse(GroupActivityCalendarSummary summary) {
        return new GroupActivityCalendarSummaryResponse(
                summary.startDate(),
                summary.endDate(),
                summary.allCount(),
                summary.halfCount(),
                summary.resetCount()
        );
    }

    private CalendarDayStatus dayStatus(LocalDate date, LocalDate today, LocalDate firstVerificationDate) {
        if (date.isAfter(today)) {
            return CalendarDayStatus.FUTURE;
        }
        if (firstVerificationDate == null || date.isBefore(firstVerificationDate)) {
            return CalendarDayStatus.NOT_STARTED;
        }
        if (date.isEqual(today)) {
            return CalendarDayStatus.IN_PROGRESS;
        }
        return CalendarDayStatus.CONFIRMED;
    }

    private int reactionCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getReactionCount();
    }

    private int afterCommentCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getAfterCommentCount();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(KST));
    }

    private record CalendarSource(
            GroupChallenge challenge,
            List<GroupActivityParticipantRow> participantRows,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals
    ) {
    }
}
