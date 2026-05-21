package com.detoxmate.group.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UsageGoalTypeRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberProfileResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(GroupMemberProfileServiceTest.FixedClockConfig.class)
class GroupMemberProfileServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 15);

    @Autowired
    GroupMemberProfileService groupMemberProfileService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    GroupMemberRepository groupMemberRepository;

    @Autowired
    GroupChallengeRepository groupChallengeRepository;

    @Autowired
    GroupChallengeParticipantRepository participantRepository;

    @Autowired
    UsageGoalTypeRepository usageGoalTypeRepository;

    @Autowired
    UserUsageGoalTimeRepository userUsageGoalTimeRepository;

    @Autowired
    ActivityRecordRepository activityRecordRepository;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    @Test
    @DisplayName("활성 그룹원은 회원 탈퇴한 과거 그룹 멤버 프로필을 익명화된 상태로 조회한다")
    void getGroupMemberProfile_withWithdrawnPastMember_returnsAnonymizedProfile() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준", "profile-images/2/profile.png"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupMember targetGroupMember = saveParticipant(group.getId(), challenge.getId(), targetUser);

        targetGroupMember.leave();
        participantRepository.findByGroupChallengeIdAndGroupMemberId(challenge.getId(), targetGroupMember.getId())
                .orElseThrow()
                .withdraw();
        targetUser.withdraw();

        GroupMemberProfileResponse response = groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                targetGroupMember.getId(),
                currentUser.getId()
        );

        assertThat(response.groupMemberId()).isEqualTo(targetGroupMember.getId());
        assertThat(response.userId()).isEqualTo(targetUser.getId());
        assertThat(response.displayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.memberStatus()).isEqualTo("LEFT");
        assertThat(response.userWithdrawn()).isTrue();
        assertThat(response.goalChangeAvailability()).isNull();
    }

    @Test
    @DisplayName("그룹 멤버 프로필은 현재 목표와 첫 인증 기준 달성률, 최근 7일 집계를 반환한다")
    void getGroupMemberProfile_returnsCurrentGoalsAndStats() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));
        UsageGoalType totalUsage = usageGoalTypeRepository.save(UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupMember targetGroupMember = saveParticipant(group.getId(), challenge.getId(), targetUser);
        GroupChallengeParticipant targetParticipant = participantRepository
                .findByGroupChallengeIdAndGroupMemberId(challenge.getId(), targetGroupMember.getId())
                .orElseThrow();
        UserUsageGoalTime goalTime = saveGoal(targetUser, totalUsage, 120, LocalDateTime.of(2026, 5, 1, 10, 0));
        saveCertifiedRecord(challenge, targetParticipant, targetUser, goalTime, TODAY.minusDays(2), 90, true);
        saveCertifiedRecord(challenge, targetParticipant, targetUser, goalTime, TODAY.minusDays(1), 140, false);

        GroupMemberProfileResponse response = groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                targetGroupMember.getId(),
                currentUser.getId()
        );

        assertThat(response.goalStatus()).isEqualTo("SET");
        assertThat(response.currentGoals())
                .extracting("id", "usageGoalType", "goalMinutes", "createdAt")
                .containsExactly(tuple(goalTime.getId(), UsageGoalTypeCode.TOTAL_USAGE, 120, LocalDateTime.of(2026, 5, 1, 10, 0)));
        assertThat(response.activitySummary().firstCertifiedDate()).isEqualTo(TODAY.minusDays(2));
        assertThat(response.activitySummary().dayCount()).isEqualTo(3);
        assertThat(response.activitySummary().achievementRate()).isEqualTo(33);
        assertThat(response.weeklySummary().totalDays()).isEqualTo(7);
        assertThat(response.weeklySummary().averageUsedMinutes()).isEqualTo(33);
        assertThat(response.weeklySummary().goalMinutes()).isEqualTo(120);
        assertThat(response.weeklySummary().differenceMinutes()).isEqualTo(87);
        assertThat(response.weeklySummary().certifiedDays()).isEqualTo(2);
        assertThat(response.weeklySummary().achievedDays()).isEqualTo(1);
        assertThat(response.goalChangeAvailability()).isNull();
    }

    @Test
    @DisplayName("목표와 인증이 없으면 미설정 상태와 0 집계를 반환한다")
    void getGroupMemberProfile_withoutGoalAndCertification_returnsEmptySummary() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        User user = userRepository.save(User.createNew("나"));
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), group.getId()));

        GroupMemberProfileResponse response = groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                groupMember.getId(),
                user.getId()
        );

        assertThat(response.goalStatus()).isEqualTo("NOT_SET");
        assertThat(response.currentGoals()).isEmpty();
        assertThat(response.goalChangeAvailability().canChange()).isTrue();
        assertThat(response.goalChangeAvailability().nextChangeAvailableDate()).isNull();
        assertThat(response.goalChangeAvailability().remainingDays()).isZero();
        assertThat(response.activitySummary().firstCertifiedDate()).isNull();
        assertThat(response.activitySummary().dayCount()).isZero();
        assertThat(response.activitySummary().achievementRate()).isZero();
        assertThat(response.weeklySummary().averageUsedMinutes()).isZero();
        assertThat(response.weeklySummary().goalMinutes()).isNull();
        assertThat(response.weeklySummary().differenceMinutes()).isNull();
        assertThat(response.weeklySummary().certifiedDays()).isZero();
        assertThat(response.weeklySummary().achievedDays()).isZero();
    }

    @Test
    @DisplayName("목표 변경 가능 여부는 본인 조회에서만 반환하고 14일 날짜 기준으로 계산한다")
    void getGroupMemberProfile_selfProfile_returnsGoalChangeAvailability() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        UsageGoalType totalUsage = usageGoalTypeRepository.save(UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE));
        User user = userRepository.save(User.createNew("나"));
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), group.getId()));
        saveGoal(user, totalUsage, 120, LocalDateTime.of(2026, 5, 2, 10, 0));

        GroupMemberProfileResponse response = groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                groupMember.getId(),
                user.getId()
        );

        assertThat(response.goalChangeAvailability().canChange()).isFalse();
        assertThat(response.goalChangeAvailability().nextChangeAvailableDate()).isEqualTo(LocalDate.of(2026, 5, 16));
        assertThat(response.goalChangeAvailability().remainingDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("첫 인증 당일은 D+1이고 실패 인증도 첫 인증 기준일에 포함한다")
    void getGroupMemberProfile_firstFailCertificationDate_countsAsFirstCertifiedDate() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));
        UsageGoalType totalUsage = usageGoalTypeRepository.save(UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE));
        User user = userRepository.save(User.createNew("나"));
        GroupMember groupMember = saveParticipant(group.getId(), challenge.getId(), user);
        GroupChallengeParticipant participant = participantRepository
                .findByGroupChallengeIdAndGroupMemberId(challenge.getId(), groupMember.getId())
                .orElseThrow();
        UserUsageGoalTime goal = saveGoal(user, totalUsage, 120, LocalDateTime.of(2026, 5, 1, 10, 0));
        saveCertifiedRecord(challenge, participant, user, goal, TODAY, 180, false);

        GroupMemberProfileResponse response = groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                groupMember.getId(),
                user.getId()
        );

        assertThat(response.activitySummary().firstCertifiedDate()).isEqualTo(TODAY);
        assertThat(response.activitySummary().dayCount()).isEqualTo(1);
        assertThat(response.activitySummary().achievementRate()).isZero();
    }

    @Test
    @DisplayName("활성 그룹원이 아니면 과거 그룹 멤버 프로필도 조회할 수 없다")
    void getGroupMemberProfile_withoutActiveRequester_throwsForbidden() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        User requester = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));
        GroupMember targetGroupMember = groupMemberRepository.save(GroupMember.createMember(targetUser.getId(), group.getId()));

        assertThatThrownBy(() -> groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                targetGroupMember.getId(),
                requester.getId()
        ))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    private GroupMember saveParticipant(Long groupId, Long challengeId, User user) {
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), groupId));
        participantRepository.save(GroupChallengeParticipant.join(groupMember.getId(), challengeId));
        return groupMember;
    }

    private UserUsageGoalTime saveGoal(User user, UsageGoalType usageGoalType, int goalMinutes, LocalDateTime createdAt) {
        UserUsageGoalTime goalTime = userUsageGoalTimeRepository.saveAndFlush(
                UserUsageGoalTime.create(user, usageGoalType, goalMinutes)
        );
        ReflectionTestUtils.setField(goalTime, "createdAt", createdAt);
        return goalTime;
    }

    private void saveCertifiedRecord(
            GroupChallenge challenge,
            GroupChallengeParticipant participant,
            User user,
            UserUsageGoalTime goalTime,
            LocalDate recordDate,
            int useMinutes,
            boolean achieved
    ) {
        ActivityRecord activityRecord = ActivityRecord.create(user, participant, "activity-records/sample.png", "오늘 기록");
        activityRecord.addDetail(goalTime, useMinutes, achieved);
        ActivityRecord savedActivityRecord = activityRecordRepository.saveAndFlush(activityRecord);
        ChallengeRecord challengeRecord = ChallengeRecord.create(challenge.getId(), participant.getId(), recordDate);
        ChallengeRecordCertificationResult result = achieved
                ? ChallengeRecordCertificationResult.SUCCESS
                : ChallengeRecordCertificationResult.FAIL;
        challengeRecord.certify(savedActivityRecord.getId(), participant.getId(), result);
        challengeRecordRepository.save(challengeRecord);
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(TODAY.atTime(9, 0).atZone(KST).toInstant(), KST);
        }
    }
}
