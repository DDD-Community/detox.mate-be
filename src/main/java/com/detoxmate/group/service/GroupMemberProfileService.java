package com.detoxmate.group.service;

import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordDetailRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberActivitySummaryResponse;
import com.detoxmate.group.dto.GroupMemberGoalChangeAvailabilityResponse;
import com.detoxmate.group.dto.GroupMemberProfileResponse;
import com.detoxmate.group.dto.GroupMemberUsageGoalResponse;
import com.detoxmate.group.dto.GroupMemberWeeklySummaryResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GroupMemberProfileService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int GOAL_CHANGE_LOCK_DAYS = 14;
    private static final int WEEKLY_SUMMARY_DAYS = 7;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupChallengeParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final ActivityRecordDetailRepository activityRecordDetailRepository;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GroupMemberProfileResponse getGroupMemberProfile(Long groupId, Long groupMemberId, Long currentUserId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."));
        groupMemberRepository.findByUserIdAndGroupIdAndStatus(currentUserId, groupId, "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 속한 그룹만 조회할 수 있습니다."));

        GroupMember targetMember = groupMemberRepository.findByIdAndGroupId(groupMemberId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹 멤버를 찾을 수 없습니다."));
        User targetUser = userRepository.findById(targetMember.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        LocalDate today = today();
        List<GroupMemberUsageGoalResponse> currentGoals = currentGoals(targetUser.getId());
        List<Long> participantIds = participantRepository.findAllByGroupMemberIdOrderByJoinedAtAscIdAsc(targetMember.getId())
                .stream()
                .map(GroupChallengeParticipant::getId)
                .toList();
        List<ChallengeRecord> allRecords = records(participantIds);
        boolean isSelfProfile = Objects.equals(targetMember.getUserId(), currentUserId);

        return new GroupMemberProfileResponse(
                targetMember.getId(),
                targetUser.getId(),
                targetMember.getGroupId(),
                targetUser.getPublicDisplayName(),
                imageReadUrlBuilder.build(targetUser.getPublicProfileImageObjectKey()),
                targetMember.getRole(),
                targetMember.getStatus(),
                targetMember.getJoinedAt(),
                currentGoals.isEmpty() ? "NOT_SET" : "SET",
                targetUser.isWithdrawn(),
                currentGoals,
                goalChangeAvailability(currentGoals, isSelfProfile, today),
                activitySummary(allRecords, today),
                weeklySummary(participantIds, currentGoals, today)
        );
    }

    private List<GroupMemberUsageGoalResponse> currentGoals(Long userId) {
        Map<UsageGoalTypeCode, UserUsageGoalTime> latestByType = new EnumMap<>(UsageGoalTypeCode.class);

        userUsageGoalTimeRepository.findAllByUser_Id(userId)
                .forEach(goalTime -> latestByType.merge(
                        goalTime.getUsageGoalType().getCode(),
                        goalTime,
                        this::laterGoal
                ));

        return latestByType.values().stream()
                .sorted(Comparator.comparing(goalTime -> goalTime.getUsageGoalType().getCode()))
                .map(goalTime -> new GroupMemberUsageGoalResponse(
                        goalTime.getId(),
                        goalTime.getUsageGoalType().getCode(),
                        goalTime.getGoalMinutes(),
                        goalTime.getCreatedAt()
                ))
                .toList();
    }

    private UserUsageGoalTime laterGoal(UserUsageGoalTime left, UserUsageGoalTime right) {
        return left.getCreatedAt().isAfter(right.getCreatedAt()) ? left : right;
    }

    private GroupMemberGoalChangeAvailabilityResponse goalChangeAvailability(
            List<GroupMemberUsageGoalResponse> currentGoals,
            boolean isSelfProfile,
            LocalDate today
    ) {
        if (!isSelfProfile) {
            return null;
        }

        if (currentGoals.isEmpty()) {
            return new GroupMemberGoalChangeAvailabilityResponse(true, null, 0);
        }

        LocalDate nextChangeAvailableDate = currentGoals.stream()
                .max(Comparator.comparing(GroupMemberUsageGoalResponse::createdAt))
                .orElseThrow()
                .createdAt()
                .toLocalDate()
                .plusDays(GOAL_CHANGE_LOCK_DAYS);
        int remainingDays = Math.max(0, (int) ChronoUnit.DAYS.between(today, nextChangeAvailableDate));

        return new GroupMemberGoalChangeAvailabilityResponse(
                !today.isBefore(nextChangeAvailableDate),
                nextChangeAvailableDate,
                remainingDays
        );
    }

    private GroupMemberActivitySummaryResponse activitySummary(List<ChallengeRecord> records, LocalDate today) {
        LocalDate firstCertifiedDate = records.stream()
                .filter(ChallengeRecord::isCertified)
                .map(ChallengeRecord::getRecordDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        if (firstCertifiedDate == null) {
            return new GroupMemberActivitySummaryResponse(null, 0, 0);
        }

        int totalDays = (int) ChronoUnit.DAYS.between(firstCertifiedDate, today) + 1;
        int achievedDays = achievedDays(records.stream()
                .filter(record -> !record.getRecordDate().isBefore(firstCertifiedDate))
                .toList());

        return new GroupMemberActivitySummaryResponse(
                firstCertifiedDate,
                totalDays,
                achievementRate(achievedDays, totalDays)
        );
    }

    private GroupMemberWeeklySummaryResponse weeklySummary(
            List<Long> participantIds,
            List<GroupMemberUsageGoalResponse> currentGoals,
            LocalDate today
    ) {
        LocalDate startDate = today.minusDays(WEEKLY_SUMMARY_DAYS - 1L);
        List<ChallengeRecord> records = records(participantIds, startDate, today);
        int averageUsedMinutes = averageTotalUsageMinutes(records);
        Integer goalMinutes = totalUsageGoalMinutes(currentGoals);

        return new GroupMemberWeeklySummaryResponse(
                startDate,
                today,
                WEEKLY_SUMMARY_DAYS,
                averageUsedMinutes,
                goalMinutes,
                goalMinutes == null ? null : goalMinutes - averageUsedMinutes,
                certifiedDays(records),
                achievedDays(records)
        );
    }

    private Integer totalUsageGoalMinutes(List<GroupMemberUsageGoalResponse> currentGoals) {
        return currentGoals.stream()
                .filter(goal -> goal.usageGoalType() == UsageGoalTypeCode.TOTAL_USAGE)
                .findFirst()
                .map(GroupMemberUsageGoalResponse::goalMinutes)
                .orElse(null);
    }

    private int averageTotalUsageMinutes(List<ChallengeRecord> records) {
        List<Long> activityRecordIds = records.stream()
                .filter(ChallengeRecord::isCertified)
                .map(ChallengeRecord::getActivityRecordId)
                .filter(Objects::nonNull)
                .toList();

        if (activityRecordIds.isEmpty()) {
            return 0;
        }

        int totalUsedMinutes = activityRecordDetailRepository.findTotalUsageDetailsByActivityRecordIds(activityRecordIds)
                .stream()
                .mapToInt(ActivityRecordDetail::getUseMinutes)
                .sum();

        return (int) Math.round(totalUsedMinutes / (double) WEEKLY_SUMMARY_DAYS);
    }

    private List<ChallengeRecord> records(List<Long> participantIds, LocalDate startDate, LocalDate endDate) {
        if (participantIds.isEmpty()) {
            return List.of();
        }

        return challengeRecordRepository.findAllByGroupChallengeParticipantIdInAndRecordDateBetweenOrderByRecordDateAscIdAsc(
                participantIds,
                startDate,
                endDate
        );
    }

    private List<ChallengeRecord> records(List<Long> participantIds) {
        if (participantIds.isEmpty()) {
            return List.of();
        }

        return challengeRecordRepository.findAllByGroupChallengeParticipantIdInOrderByRecordDateAscIdAsc(participantIds);
    }

    private int certifiedDays(List<ChallengeRecord> records) {
        return (int) records.stream()
                .filter(ChallengeRecord::isCertified)
                .map(ChallengeRecord::getRecordDate)
                .distinct()
                .count();
    }

    private int achievedDays(List<ChallengeRecord> records) {
        return (int) records.stream()
                .filter(ChallengeRecord::isCertificationSucceeded)
                .map(ChallengeRecord::getRecordDate)
                .distinct()
                .count();
    }

    private int achievementRate(int achievedDays, int totalDays) {
        if (totalDays <= 0) {
            return 0;
        }

        return (int) Math.round(achievedDays * 100.0 / totalDays);
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(KST));
    }
}
