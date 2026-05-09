package com.detoxmate.group.service;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.group.GroupActivityCalendarErrorCode;
import com.detoxmate.group.activity.GroupActivityCalendarSummary;
import com.detoxmate.group.activity.GroupActivityParticipant;
import com.detoxmate.group.activity.GroupActivityVerificationPolicy;
import com.detoxmate.group.activity.MemberDailyGoal;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.dto.GroupActivityCalendarResponse;
import com.detoxmate.group.dto.GroupActivityCalendarSummaryResponse;
import com.detoxmate.group.dto.GroupActivityParticipantRow;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final GroupActivityVerificationPolicy verificationPolicy;
    private final Clock clock;

    public GroupActivityCalendarResponse getCalendar(Long groupChallengeId, Long currentUserId) {
        GroupActivityContext activity = loadActivityContextByChallengeId(groupChallengeId, currentUserId);
        LocalDate today = today();
        LocalDate firstVerificationDate = firstVerificationDate(activity, today);
        Map<LocalDate, Set<Long>> certifiedParticipantIdsByDate = certifiedParticipantIdsByDate(
                groupChallengeId,
                firstVerificationDate,
                today
        );

        GroupActivityCalendarSummary summary = verificationPolicy.summarizeConfirmedDates(
                today,
                firstVerificationDate,
                activity.participants(),
                activity.goals(),
                certifiedParticipantIdsByDate
        );
        int streakDays = verificationPolicy.streakDays(
                today,
                firstVerificationDate,
                activity.participants(),
                activity.goals(),
                certifiedParticipantIdsByDate
        );

        return new GroupActivityCalendarResponse(
                activity.challenge().getGroupId(),
                firstVerificationDate,
                streakDays,
                toSummaryResponse(summary)
        );
    }

    public int getStreakDays(Long groupChallengeId, Long currentUserId) {
        GroupActivityContext activity = loadActivityContextByChallengeId(groupChallengeId, currentUserId);
        LocalDate today = today();
        LocalDate firstVerificationDate = firstVerificationDate(activity, today);
        Map<LocalDate, Set<Long>> certifiedParticipantIdsByDate = certifiedParticipantIdsByDate(
                groupChallengeId,
                firstVerificationDate,
                today
        );

        return verificationPolicy.streakDays(
                today,
                firstVerificationDate,
                activity.participants(),
                activity.goals(),
                certifiedParticipantIdsByDate
        );
    }

    private GroupActivityContext loadActivityContext(Long groupId, Long currentUserId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(GroupActivityCalendarErrorCode.GROUP_NOT_FOUND));
        validateGroupAccess(groupId, currentUserId);

        GroupChallenge challenge = groupChallengeRepository.findTopByGroupIdOrderByChallengeNoDesc(groupId)
                .orElseThrow(() -> new CustomException(GroupActivityCalendarErrorCode.GROUP_CHALLENGE_NOT_FOUND));
        return loadActivityContext(challenge);
    }

    private GroupActivityContext loadActivityContextByChallengeId(Long groupChallengeId, Long currentUserId) {
        GroupChallenge challenge = groupChallengeRepository.findById(groupChallengeId)
                .orElseThrow(() -> new CustomException(GroupActivityCalendarErrorCode.GROUP_CHALLENGE_NOT_FOUND));
        groupRepository.findById(challenge.getGroupId())
                .orElseThrow(() -> new CustomException(GroupActivityCalendarErrorCode.GROUP_NOT_FOUND));
        validateGroupAccess(challenge.getGroupId(), currentUserId);

        return loadActivityContext(challenge);
    }

    private GroupActivityContext loadActivityContext(GroupChallenge challenge) {
        List<GroupActivityParticipantRow> participantRows =
                participantRepository.findActivityParticipantRowsByGroupChallengeId(challenge.getId());
        List<GroupActivityParticipant> participants = participantRows.stream()
                .map(this::toParticipant)
                .toList();
        List<MemberDailyGoal> goals = memberDailyGoals(participantRows);

        return new GroupActivityContext(challenge, participantRows, participants, goals);
    }

    private void validateGroupAccess(Long groupId, Long currentUserId) {
        groupMemberRepository.findByUserIdAndGroupIdAndStatus(currentUserId, groupId, ACTIVE_MEMBER_STATUS)
                .orElseThrow(() -> new CustomException(GroupActivityCalendarErrorCode.GROUP_ACCESS_DENIED));
    }

    private LocalDate firstVerificationDate(GroupActivityContext activity, LocalDate today) {
        return verificationPolicy.firstVerificationDate(
                activity.participants(),
                activity.goals(),
                today
        );
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

    private GroupActivityCalendarSummaryResponse toSummaryResponse(GroupActivityCalendarSummary summary) {
        return new GroupActivityCalendarSummaryResponse(
                summary.startDate(),
                summary.endDate(),
                summary.allCount(),
                summary.halfCount(),
                summary.resetCount()
        );
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(KST));
    }

    private record GroupActivityContext(
            GroupChallenge challenge,
            List<GroupActivityParticipantRow> participantRows,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals
    ) {
    }
}
