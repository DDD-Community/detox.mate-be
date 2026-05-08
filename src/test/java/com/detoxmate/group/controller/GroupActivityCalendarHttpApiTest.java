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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(GroupActivityCalendarHttpApiTest.FixedClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GroupActivityCalendarHttpApiTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 16);

    @LocalServerPort
    int port;

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
    JdbcTemplate jdbcTemplate;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("그룹 활동 캘린더 API를 실제 HTTP 요청으로 호출한다")
    void groupActivityCalendarApis_workThroughHttp() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();
        String bearer = "Bearer " + jwtTokenProvider.createAccessToken(fixture.currentUser().getId());

        HttpResponse<String> calendarResponse = sendGet(
                "/groups/" + fixture.group().getId() + "/activity-calendar",
                bearer
        );
        HttpResponse<String> historyResponse = sendGet(
                "/groups/" + fixture.group().getId() + "/activity-calendar/days/2026-04-13",
                bearer
        );

        assertThat(calendarResponse.statusCode()).isEqualTo(200);
        assertThat(calendarResponse.body()).contains("\"firstVerificationDate\":\"2026-04-12\"");
        assertThat(historyResponse.statusCode()).isEqualTo(200);
        assertThat(historyResponse.body()).contains(
                "\"dayStatus\":\"CONFIRMED\"",
                "\"result\":\"HALF\"",
                "\"displayName\":\"서연\"",
                "\"dailyStatus\":\"NOT_ACTIVE\""
        );
    }

    private HttpResponse<String> sendGet(String path, String bearer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", bearer)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private CalendarFixture saveCalendarFixture() {
        UsageGoalType totalUsage = usageGoalTypeRepository.save(UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE));
        Group group = groupRepository.save(Group.createNew("실호출", "HTTPC"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User certifiedUser = userRepository.save(User.createNew("지수", "profiles/jisu.png"));
        User currentUser = userRepository.save(User.createNew("나", "profiles/me.png"));
        User leftUser = userRepository.save(User.createNew("서연", "profiles/seoyeon.png"));

        LocalDateTime joinedAt = LocalDateTime.of(2026, 4, 10, 10, 0);
        GroupChallengeParticipant certifiedParticipant = saveParticipant(group, challenge, certifiedUser, joinedAt);
        saveParticipant(group, challenge, currentUser, joinedAt.plusMinutes(1));
        saveInactiveParticipant(group, challenge, leftUser, joinedAt.plusMinutes(2), LocalDateTime.of(2026, 4, 13, 11, 0));

        UserUsageGoalTime certifiedGoal = saveGoal(certifiedUser, totalUsage, 90, LocalDateTime.of(2026, 4, 11, 9, 0));
        saveGoal(currentUser, totalUsage, 80, LocalDateTime.of(2026, 4, 11, 9, 0));
        saveGoal(leftUser, totalUsage, 70, LocalDateTime.of(2026, 4, 12, 9, 0));

        certify(challenge, certifiedParticipant, certifiedUser, certifiedGoal, LocalDate.of(2026, 4, 13));

        return new CalendarFixture(group, currentUser);
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
            LocalDate recordDate
    ) {
        ChallengeRecord challengeRecord = challengeRecordRepository.saveAndFlush(
                ChallengeRecord.create(challenge.getId(), participant.getId(), recordDate)
        );
        ActivityRecord activityRecord = ActivityRecord.create(
                user,
                participant,
                "activity/" + user.getId() + "/" + recordDate + ".png",
                "2시간동안 러닝 뛰고 온 날!"
        );
        activityRecord.addDetail(goal, 70, true);
        ActivityRecord savedActivityRecord = activityRecordRepository.saveAndFlush(activityRecord);

        challengeRecord.certify(
                savedActivityRecord.getId(),
                participant.getId(),
                ChallengeRecordCertificationResult.SUCCESS
        );
        challengeRecordRepository.saveAndFlush(challengeRecord);
    }

    private record CalendarFixture(Group group, User currentUser) {
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
