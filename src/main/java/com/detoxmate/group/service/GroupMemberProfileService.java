package com.detoxmate.group.service;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberProfileResponse;
import com.detoxmate.group.dto.GroupMemberUsageGoalResponse;
import com.detoxmate.group.dto.MemberOverallStatsResponse;
import com.detoxmate.group.dto.MemberRecent7DaysStatsResponse;
import com.detoxmate.group.dto.MemberStatsResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupMemberProfileService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final GroupChallengeParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GroupMemberProfileResponse getGroupMemberProfile(Long groupId, Long memberId, Long currentUserId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."));
        groupMemberRepository.findByUserIdAndGroupIdAndStatus(currentUserId, groupId, "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 속한 그룹만 조회할 수 있습니다."));

        GroupMember targetMember = groupMemberRepository.findByIdAndGroupId(memberId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹 멤버를 찾을 수 없습니다."));
        User targetUser = userRepository.findById(targetMember.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        GroupChallenge latestChallenge = groupChallengeRepository.findTopByGroupIdOrderByChallengeNoDesc(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹 챌린지를 찾을 수 없습니다."));
        Optional<GroupChallengeParticipant> participant =
                participantRepository.findByGroupChallengeIdAndGroupMemberId(latestChallenge.getId(), targetMember.getId());

        List<GroupMemberUsageGoalResponse> currentGoals = currentGoals(targetUser.getId());
        LocalDate today = today();

        return new GroupMemberProfileResponse(
                targetMember.getId(),
                targetUser.getId(),
                targetMember.getGroupId(),
                targetUser.getPublicDisplayName(),
                imageReadUrlBuilder.build(targetUser.getPublicProfileImageObjectKey()),
                targetMember.getRole(),
                targetMember.getStatus(),
                targetMember.getJoinedAt(),
                dayCount(participant, targetMember, today),
                targetUser.isWithdrawn(),
                currentGoals,
                stats(participant, currentGoals, today)
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

    private int dayCount(Optional<GroupChallengeParticipant> participant, GroupMember targetMember, LocalDate today) {
        LocalDate joinedDate = participant
                .map(GroupChallengeParticipant::getJoinedAt)
                .orElse(targetMember.getJoinedAt())
                .toLocalDate();

        return Math.max(0, (int) ChronoUnit.DAYS.between(joinedDate, today));
    }

    private MemberStatsResponse stats(
            Optional<GroupChallengeParticipant> participant,
            List<GroupMemberUsageGoalResponse> currentGoals,
            LocalDate today
    ) {
        LocalDate recentStartDate = today.minusDays(6);

        if (participant.isEmpty() || currentGoals.isEmpty()) {
            return new MemberStatsResponse(
                    new MemberOverallStatsResponse(today, today, 0, 0, 0),
                    new MemberRecent7DaysStatsResponse(recentStartDate, today, 7, 0, 0)
            );
        }

        LocalDate overallStartDate = overallStartDate(currentGoals);
        List<ChallengeRecord> overallRecords = records(participant.get(), overallStartDate, today);
        List<ChallengeRecord> recentRecords = records(participant.get(), recentStartDate, today);

        int overallTotalDays = (int) ChronoUnit.DAYS.between(overallStartDate, today) + 1;
        int overallAchievedDays = achievedDays(overallRecords);

        return new MemberStatsResponse(
                new MemberOverallStatsResponse(
                        overallStartDate,
                        today,
                        overallTotalDays,
                        overallAchievedDays,
                        achievementRate(overallAchievedDays, overallTotalDays)
                ),
                new MemberRecent7DaysStatsResponse(
                        recentStartDate,
                        today,
                        7,
                        submittedDays(recentRecords),
                        achievedDays(recentRecords)
                )
        );
    }

    private LocalDate overallStartDate(List<GroupMemberUsageGoalResponse> currentGoals) {
        return currentGoals.stream()
                .filter(goal -> goal.usageGoalType() == UsageGoalTypeCode.TOTAL_USAGE)
                .findFirst()
                .or(() -> currentGoals.stream().max(Comparator.comparing(GroupMemberUsageGoalResponse::setAt)))
                .orElseThrow()
                .setAt()
                .toLocalDate();
    }

    private List<ChallengeRecord> records(GroupChallengeParticipant participant, LocalDate startDate, LocalDate endDate) {
        return challengeRecordRepository.findAllByGroupChallengeParticipantIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(
                participant.getId(),
                startDate,
                endDate
        );
    }

    private int submittedDays(List<ChallengeRecord> records) {
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
