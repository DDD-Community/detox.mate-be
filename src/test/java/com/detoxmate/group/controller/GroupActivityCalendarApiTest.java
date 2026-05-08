package com.detoxmate.group.controller;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UsageGoalTypeRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
@Import(GroupActivityCalendarApiTest.FixedClockConfig.class)
class GroupActivityCalendarApiTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 16);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

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
    ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    ActivityRecordRepository activityRecordRepository;

    @Autowired
    ChallengeRecordStatusCountRepository statusCountRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("GET /groups/{groupId}/activity-calendar — 첫 인증 시작일 이후 누적 요약과 스트릭을 반환한다")
    void getActivityCalendar_returnsCumulativeSummarySinceFirstVerificationDate() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        mockMvc.perform(get("/groups/{groupId}/activity-calendar", fixture.group().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(fixture.group().getId()))
                .andExpect(jsonPath("$.firstVerificationDate").value("2026-04-12"))
                .andExpect(jsonPath("$.streakDays").value(1))
                .andExpect(jsonPath("$.summary.startDate").value("2026-04-12"))
                .andExpect(jsonPath("$.summary.endDate").value("2026-04-15"))
                .andExpect(jsonPath("$.summary.allCount").value(1))
                .andExpect(jsonPath("$.summary.halfCount").value(2))
                .andExpect(jsonPath("$.summary.resetCount").value(1));
    }

    @Test
    @DisplayName("GET /groups/{groupId}/activity-calendar/days/{date} — 날짜별 멤버 상태, 목표, 인증 내용을 반환한다")
    void getActivityCalendarHistory_returnsMemberStatusGoalsAndActivityRecords() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        mockMvc.perform(get(
                        "/groups/{groupId}/activity-calendar/days/{date}",
                        fixture.group().getId(),
                        "2026-04-13"
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(fixture.group().getId()))
                .andExpect(jsonPath("$.date").value("2026-04-13"))
                .andExpect(jsonPath("$.dailySummary.dayStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.dailySummary.result").value("HALF"))
                .andExpect(jsonPath("$.dailySummary.activeMemberCount").value(3))
                .andExpect(jsonPath("$.dailySummary.certifiedMemberCount").value(2))
                .andExpect(jsonPath("$.dailySummary.requiredCount").value(2))
                .andExpect(jsonPath("$.members.length()").value(4))
                .andExpect(jsonPath("$.members[0].displayName").value("민준"))
                .andExpect(jsonPath("$.members[0].dailyStatus").value("GOAL_FAILED"))
                .andExpect(jsonPath("$.members[0].activityRecord.allAchieved").value(false))
                .andExpect(jsonPath("$.members[0].activityRecord.details[0].usageGoalType").value("TOTAL_USAGE"))
                .andExpect(jsonPath("$.members[1].displayName").value("지수"))
                .andExpect(jsonPath("$.members[1].dailyStatus").value("GOAL_ACHIEVED"))
                .andExpect(jsonPath("$.members[1].goals[0].effectiveDate").value("2026-04-12"))
                .andExpect(jsonPath("$.members[1].activityRecord.allAchieved").value(true))
                .andExpect(jsonPath("$.members[2].displayName").value("나"))
                .andExpect(jsonPath("$.members[2].isMe").value(true))
                .andExpect(jsonPath("$.members[2].dailyStatus").value("NOT_CERTIFIED"))
                .andExpect(jsonPath("$.members[2].activityRecord").doesNotExist())
                .andExpect(jsonPath("$.members[3].displayName").value("서연"))
                .andExpect(jsonPath("$.members[3].memberStatus").value("LEFT"))
                .andExpect(jsonPath("$.members[3].participantStatus").value("WITHDRAWN"))
                .andExpect(jsonPath("$.members[3].dailyStatus").value("NOT_ACTIVE"))
                .andExpect(jsonPath("$.members[3].includedInGroupResult").value(false));
    }

    @Test
    @DisplayName("GET /groups/{groupId}/activity-calendar/days/{date} — 오늘 히스토리는 홈 피드처럼 활동중 멤버의 빈 챌린지 기록을 보장한다")
    void getActivityCalendarHistory_todayCreatesEmptyChallengeRecordsForActiveMembers() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        assertThat(challengeRecordRepository.findAllByGroupChallengeDate(fixture.challenge().getId(), TODAY))
                .isEmpty();

        mockMvc.perform(get(
                        "/groups/{groupId}/activity-calendar/days/{date}",
                        fixture.group().getId(),
                        TODAY
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailySummary.dayStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.dailySummary.result").doesNotExist())
                .andExpect(jsonPath("$.dailySummary.activeMemberCount").value(3))
                .andExpect(jsonPath("$.members.length()").value(4))
                .andExpect(jsonPath("$.members[0].displayName").value("나"))
                .andExpect(jsonPath("$.members[1].displayName").value("민준"))
                .andExpect(jsonPath("$.members[2].displayName").value("서연"))
                .andExpect(jsonPath("$.members[2].dailyStatus").value("NOT_ACTIVE"))
                .andExpect(jsonPath("$.members[3].displayName").value("지수"));

        List<ChallengeRecord> todayRecords = challengeRecordRepository.findAllByGroupChallengeDate(
                fixture.challenge().getId(),
                TODAY
        );

        assertThat(todayRecords).hasSize(3);
        assertThat(todayRecords)
                .allSatisfy(record -> assertThat(statusCountRepository.findByChallengeRecordId(record.getId()))
                        .isPresent());
    }

    private CalendarFixture saveCalendarFixture() {
        UsageGoalType totalUsage = usageGoalTypeRepository.save(UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE));
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User jisu = userRepository.save(User.createNew("지수", "profiles/jisu.png"));
        User minjun = userRepository.save(User.createNew("민준", "profiles/minjun.png"));
        User currentUser = userRepository.save(User.createNew("나", "profiles/me.png"));
        User leftUser = userRepository.save(User.createNew("서연", "profiles/seoyeon.png"));

        LocalDateTime joinedAt = LocalDateTime.of(2026, 4, 10, 10, 0);
        GroupChallengeParticipant jisuParticipant = saveParticipant(group, challenge, jisu, joinedAt);
        GroupChallengeParticipant minjunParticipant = saveParticipant(group, challenge, minjun, joinedAt.plusMinutes(1));
        GroupChallengeParticipant currentParticipant = saveParticipant(group, challenge, currentUser, joinedAt.plusMinutes(2));
        saveInactiveParticipant(
                group,
                challenge,
                leftUser,
                joinedAt.plusMinutes(3),
                LocalDateTime.of(2026, 4, 13, 11, 0)
        );

        UserUsageGoalTime jisuGoal = saveGoal(jisu, totalUsage, 90, LocalDateTime.of(2026, 4, 11, 9, 0));
        UserUsageGoalTime minjunGoal = saveGoal(minjun, totalUsage, 120, LocalDateTime.of(2026, 4, 11, 9, 0));
        UserUsageGoalTime currentGoal = saveGoal(currentUser, totalUsage, 80, LocalDateTime.of(2026, 4, 11, 9, 0));
        saveGoal(leftUser, totalUsage, 70, LocalDateTime.of(2026, 4, 12, 9, 0));

        certify(challenge, jisuParticipant, jisu, jisuGoal, LocalDate.of(2026, 4, 12), 70, true);
        certify(challenge, minjunParticipant, minjun, minjunGoal, LocalDate.of(2026, 4, 12), 90, true);
        certify(challenge, currentParticipant, currentUser, currentGoal, LocalDate.of(2026, 4, 12), 60, true);

        certify(challenge, jisuParticipant, jisu, jisuGoal, LocalDate.of(2026, 4, 13), 70, true);
        certify(challenge, minjunParticipant, minjun, minjunGoal, LocalDate.of(2026, 4, 13), 365, false);

        certify(challenge, jisuParticipant, jisu, jisuGoal, LocalDate.of(2026, 4, 14), 70, true);

        certify(challenge, jisuParticipant, jisu, jisuGoal, LocalDate.of(2026, 4, 15), 70, true);
        certify(challenge, minjunParticipant, minjun, minjunGoal, LocalDate.of(2026, 4, 15), 80, true);

        return new CalendarFixture(group, challenge, currentUser);
    }

    private GroupChallengeParticipant saveParticipant(
            Group group,
            GroupChallenge challenge,
            User user,
            LocalDateTime joinedAt
    ) {
        GroupMember groupMember = GroupMember.createMember(user.getId(), group.getId());
        ReflectionTestUtils.setField(groupMember, "joinedAt", joinedAt);
        GroupMember savedGroupMember = groupMemberRepository.save(groupMember);

        GroupChallengeParticipant participant = GroupChallengeParticipant.join(savedGroupMember.getId(), challenge.getId());
        ReflectionTestUtils.setField(participant, "joinedAt", joinedAt);
        return participantRepository.save(participant);
    }

    private GroupChallengeParticipant saveInactiveParticipant(
            Group group,
            GroupChallenge challenge,
            User user,
            LocalDateTime joinedAt,
            LocalDateTime leftAt
    ) {
        GroupMember groupMember = GroupMember.createMember(user.getId(), group.getId());
        ReflectionTestUtils.setField(groupMember, "status", "LEFT");
        ReflectionTestUtils.setField(groupMember, "joinedAt", joinedAt);
        ReflectionTestUtils.setField(groupMember, "leftAt", leftAt);
        GroupMember savedGroupMember = groupMemberRepository.save(groupMember);

        GroupChallengeParticipant participant = GroupChallengeParticipant.join(savedGroupMember.getId(), challenge.getId());
        ReflectionTestUtils.setField(participant, "status", "WITHDRAWN");
        ReflectionTestUtils.setField(participant, "joinedAt", joinedAt);
        ReflectionTestUtils.setField(participant, "withdrawnAt", leftAt);
        return participantRepository.save(participant);
    }

    private UserUsageGoalTime saveGoal(
            User user,
            UsageGoalType usageGoalType,
            int goalMinutes,
            LocalDateTime setAt
    ) {
        UserUsageGoalTime goal = userUsageGoalTimeRepository.saveAndFlush(
                UserUsageGoalTime.create(user, usageGoalType, goalMinutes)
        );
        jdbcTemplate.update(
                "UPDATE user_usage_goal_times SET created_at = ?, updated_at = ? WHERE user_usage_goal_times_id = ?",
                Timestamp.valueOf(setAt),
                Timestamp.valueOf(setAt),
                goal.getId()
        );
        ReflectionTestUtils.setField(goal, "createdAt", setAt);
        ReflectionTestUtils.setField(goal, "updatedAt", setAt);
        return goal;
    }

    private void certify(
            GroupChallenge challenge,
            GroupChallengeParticipant participant,
            User user,
            UserUsageGoalTime goal,
            LocalDate recordDate,
            int usedMinutes,
            boolean achieved
    ) {
        ChallengeRecord challengeRecord = challengeRecordRepository.saveAndFlush(
                ChallengeRecord.create(challenge.getId(), participant.getId(), recordDate)
        );
        ActivityRecord activityRecord = ActivityRecord.create(
                user,
                participant,
                "activity/" + user.getId() + "/" + recordDate + ".png",
                achieved ? "2시간동안 러닝 뛰고 온 날!" : "릴스 무한루프에 빠졌어요..."
        );
        activityRecord.addDetail(goal, usedMinutes, achieved);
        ActivityRecord savedActivityRecord = activityRecordRepository.saveAndFlush(activityRecord);

        challengeRecord.certify(
                savedActivityRecord.getId(),
                participant.getId(),
                achieved ? ChallengeRecordCertificationResult.SUCCESS : ChallengeRecordCertificationResult.FAIL
        );
        challengeRecordRepository.saveAndFlush(challengeRecord);
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    private record CalendarFixture(Group group, GroupChallenge challenge, User currentUser) {
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
