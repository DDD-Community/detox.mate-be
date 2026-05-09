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
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupActivityParticipantRow;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.repository.PokeRepository;
import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.domain.ReactionBody;
import com.detoxmate.reaction.repository.ReactionRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    ReactionRepository reactionRepository;

    @Autowired
    PokeRepository pokeRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("GET /group-challenges/{groupChallengeId}/activity-calendar — 첫 인증 시작일 이후 누적 요약과 스트릭을 반환한다")
    void getActivityCalendar_returnsCumulativeSummarySinceFirstVerificationDate() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/activity-calendar", fixture.challenge().getId())
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
    @DisplayName("GET /group-challenges/{groupChallengeId}/challenge-records?date={date} — 히스토리 피드는 기존 challenge-records API와 연동할 기록 ID를 제공한다")
    void getChallengeRecordsHistory_returnsChallengeRecordIdForExistingRecord() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/challenge-records", fixture.challenge().getId())
                        .queryParam("date", "2026-04-13")
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
                .andExpect(jsonPath("$.members[0].challengeRecordId").isNumber())
                .andExpect(jsonPath("$.members[0].challengeStatus").doesNotExist())
                .andExpect(jsonPath("$.members[0].activityRecord.id").doesNotExist())
                .andExpect(jsonPath("$.members[0].activityRecord.allAchieved").value(false))
                .andExpect(jsonPath("$.members[0].activityRecord.details[0].usageGoalType").value("TOTAL_USAGE"))
                .andExpect(jsonPath("$.members[0].activityRecord.reactionCount").doesNotExist())
                .andExpect(jsonPath("$.members[0].activityRecord.commentCount").doesNotExist())
                .andExpect(jsonPath("$.members[0].reactionCount").value(22))
                .andExpect(jsonPath("$.members[0].reactions").doesNotExist())
                .andExpect(jsonPath("$.members[0].commentCount").value(10))
                .andExpect(jsonPath("$.members[0].pokeCount").value(0))
                .andExpect(jsonPath("$.members[0].isPoked").value(false))
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
    @DisplayName("GET /group-challenges/{groupChallengeId}/challenge-records/today — 홈 피드는 활성 멤버만 조회하고 빈 기록을 만든다")
    void getChallengeRecordsToday_returnsActiveMembersAndCreatesEmptyRecords() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        assertThat(challengeRecordRepository.findAllByGroupChallengeDate(fixture.challenge().getId(), TODAY))
                .isEmpty();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/challenge-records/today", fixture.challenge().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailySummary.dayStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.dailySummary.result").doesNotExist())
                .andExpect(jsonPath("$.dailySummary.activeMemberCount").value(3))
                .andExpect(jsonPath("$.members.length()").value(3))
                .andExpect(jsonPath("$.members[0].displayName").value("나"))
                .andExpect(jsonPath("$.members[0].dailyStatus").value("NOT_CERTIFIED"))
                .andExpect(jsonPath("$.members[0].challengeRecordId").isNumber())
                .andExpect(jsonPath("$.members[0].challengeStatus").doesNotExist())
                .andExpect(jsonPath("$.members[0].activityRecord").doesNotExist())
                .andExpect(jsonPath("$.members[0].reactionCount").value(0))
                .andExpect(jsonPath("$.members[0].commentCount").value(0))
                .andExpect(jsonPath("$.members[0].pokeCount").value(0))
                .andExpect(jsonPath("$.members[0].isPoked").value(false))
                .andExpect(jsonPath("$.members[1].displayName").value("민준"))
                .andExpect(jsonPath("$.members[2].displayName").value("지수"))
                .andExpect(jsonPath("$.members[?(@.dailyStatus == 'NOT_ACTIVE')]").isEmpty());

        List<ChallengeRecord> todayRecords = challengeRecordRepository.findAllByGroupChallengeDate(
                fixture.challenge().getId(),
                TODAY
        );

        assertThat(todayRecords).hasSize(3);
        assertThat(todayRecords)
                .allSatisfy(record -> assertThat(statusCountRepository.findByChallengeRecordId(record.getId()))
                        .isPresent());
    }

    @Test
    @DisplayName("GET /group-challenges/{groupChallengeId}/challenge-records?date={today} — 히스토리 피드는 오늘 날짜를 거부한다")
    void getChallengeRecordsHistory_todayReturnsBadRequest() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/challenge-records", fixture.challenge().getId())
                        .queryParam("date", TODAY.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEED_HISTORY_DATE_MUST_BE_PAST"));
    }

    @Test
    @DisplayName("GET /group-challenges/{groupChallengeId}/challenge-records?date={future} — 히스토리 피드는 미래 날짜를 거부한다")
    void getChallengeRecordsHistory_futureReturnsBadRequest() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/challenge-records", fixture.challenge().getId())
                        .queryParam("date", TODAY.plusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEED_HISTORY_DATE_MUST_BE_PAST"));
    }

    @Test
    @DisplayName("히스토리 피드 카드의 challengeRecordId로 기존 피드 상세를 조회한다")
    void getFeedDetail_fromHistoryChallengeRecordIdReturnsExistingDetail() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();

        MvcResult historyResult = mockMvc.perform(
                        get("/group-challenges/{groupChallengeId}/challenge-records", fixture.challenge().getId())
                                .queryParam("date", "2026-04-13")
                                .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andReturn();

        Number challengeRecordId = JsonPath.read(
                historyResult.getResponse().getContentAsString(),
                "$.members[0].challengeRecordId"
        );

        mockMvc.perform(get("/challenge-records/{challengeRecordId}", challengeRecordId.longValue())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeRecordId").value(challengeRecordId.longValue()))
                .andExpect(jsonPath("$.groupChallengeId").value(fixture.challenge().getId()))
                .andExpect(jsonPath("$.activityRecordId").isNumber())
                .andExpect(jsonPath("$.challengeStatus").value("AFTER_RECORD_FAIL"))
                .andExpect(jsonPath("$.recordDate").value("2026-04-13"))
                .andExpect(jsonPath("$.author.displayName").value("민준"))
                .andExpect(jsonPath("$.activityCreatedAt").exists())
                .andExpect(jsonPath("$.activityImageUrl").exists())
                .andExpect(jsonPath("$.oneLineReview").value("릴스 무한루프에 빠졌어요..."))
                .andExpect(jsonPath("$.goalStatus").value("FAIL"))
                .andExpect(jsonPath("$.snapshotGoalMinutes").value(120))
                .andExpect(jsonPath("$.details[0].usageGoalTypeCode").value("TOTAL_USAGE"))
                .andExpect(jsonPath("$.details[0].usedMinutes").value(365))
                .andExpect(jsonPath("$.reactions.totalCount").value(2))
                .andExpect(jsonPath("$.reactions.summary[0].reactionBody").value("MUSCLE"))
                .andExpect(jsonPath("$.reactions.summary[0].displayName").value("지수"))
                .andExpect(jsonPath("$.reactions.summary[1].reactionBody").value("CLAP"))
                .andExpect(jsonPath("$.reactions.summary[1].displayName").value("나"))
                .andExpect(jsonPath("$.commentCount").value(10))
                .andExpect(jsonPath("$.pokeCount").value(0))
                .andExpect(jsonPath("$.pokeable").value(false))
                .andExpect(jsonPath("$.poked").value(false))
                .andExpect(jsonPath("$.pokedUsers").isEmpty());
    }

    @Test
    @DisplayName("홈 피드 카드의 challengeRecordId로 인증 전 상세와 댓글/콕 API를 연동한다")
    void getFeedDetail_fromTodayChallengeRecordIdSupportsPokesAndComments() throws Exception {
        CalendarFixture fixture = saveCalendarFixture();
        GroupActivityParticipantRow minjunRow = participantRowOf(fixture.challenge(), "민준");

        MvcResult todayResult = mockMvc.perform(
                        get("/group-challenges/{groupChallengeId}/challenge-records/today", fixture.challenge().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andReturn();

        Number challengeRecordId = JsonPath.read(
                todayResult.getResponse().getContentAsString(),
                "$.members[1].challengeRecordId"
        );

        statusCountRepository.increasePokeCount(challengeRecordId.longValue());
        pokeRepository.save(Poke.create(
                challengeRecordId.longValue(),
                fixture.currentUser().getId(),
                minjunRow.userId(),
                TODAY
        ));

        mockMvc.perform(post("/challenge-records/{challengeRecordId}/comments", challengeRecordId.longValue())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "commentBody": "인증 기다리고 있어요" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challengeRecordId").value(challengeRecordId.longValue()))
                .andExpect(jsonPath("$.commentBody").value("인증 기다리고 있어요"));

        mockMvc.perform(get("/challenge-records/{challengeRecordId}/comments", challengeRecordId.longValue())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].commentBody").value("인증 기다리고 있어요"));

        mockMvc.perform(get("/challenge-records/{challengeRecordId}", challengeRecordId.longValue())
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.currentUser().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeRecordId").value(challengeRecordId.longValue()))
                .andExpect(jsonPath("$.challengeStatus").value("BEFORE_RECORD"))
                .andExpect(jsonPath("$.recordDate").value("2026-04-16"))
                .andExpect(jsonPath("$.author.displayName").value("민준"))
                .andExpect(jsonPath("$.activityRecordId").doesNotExist())
                .andExpect(jsonPath("$.activityCreatedAt").doesNotExist())
                .andExpect(jsonPath("$.activityImageUrl").doesNotExist())
                .andExpect(jsonPath("$.oneLineReview").doesNotExist())
                .andExpect(jsonPath("$.goalStatus").doesNotExist())
                .andExpect(jsonPath("$.snapshotGoalMinutes").doesNotExist())
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.reactions.totalCount").value(0))
                .andExpect(jsonPath("$.reactions.summary").isEmpty())
                .andExpect(jsonPath("$.commentCount").value(1))
                .andExpect(jsonPath("$.pokeCount").value(1))
                .andExpect(jsonPath("$.pokeable").value(true))
                .andExpect(jsonPath("$.poked").value(true))
                .andExpect(jsonPath("$.pokedUsers[0].displayName").value("나"));
    }

    private CalendarFixture saveCalendarFixture() {
        UsageGoalType totalUsage = usageGoalTypeRepository.save(UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE));
        saveUnusedGroupChallenges();

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
        ChallengeRecord minjunApril13Record = certify(
                challenge,
                minjunParticipant,
                minjun,
                minjunGoal,
                LocalDate.of(2026, 4, 13),
                365,
                false
        );
        reactionRepository.save(Reaction.create(minjunApril13Record.getId(), currentUser.getId(), ReactionBody.CLAP));
        reactionRepository.save(Reaction.create(minjunApril13Record.getId(), jisu.getId(), ReactionBody.MUSCLE));

        certify(challenge, jisuParticipant, jisu, jisuGoal, LocalDate.of(2026, 4, 14), 70, true);

        certify(challenge, jisuParticipant, jisu, jisuGoal, LocalDate.of(2026, 4, 15), 70, true);
        certify(challenge, minjunParticipant, minjun, minjunGoal, LocalDate.of(2026, 4, 15), 80, true);

        return new CalendarFixture(group, challenge, currentUser);
    }

    private void saveUnusedGroupChallenges() {
        Group unusedGroup = groupRepository.save(Group.createNew("미사용 방", "UNUSD"));
        groupChallengeRepository.save(GroupChallenge.createFirst(unusedGroup.getId()));
        groupChallengeRepository.save(GroupChallenge.createFirst(unusedGroup.getId()));
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

    private ChallengeRecord certify(
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

        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(challengeRecord.getId());
        ReflectionTestUtils.setField(statusCount, "afterCommentCount", 10);
        ReflectionTestUtils.setField(statusCount, "reactionCount", 22);
        statusCountRepository.saveAndFlush(statusCount);

        return challengeRecord;
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    private Long groupMemberIdOf(GroupChallenge challenge, String displayName) {
        return participantRowOf(challenge, displayName).groupMemberId();
    }

    private GroupActivityParticipantRow participantRowOf(GroupChallenge challenge, String displayName) {
        return participantRepository.findActivityParticipantRowsByGroupChallengeId(challenge.getId()).stream()
                .filter(row -> displayName.equals(row.displayName()))
                .findFirst()
                .orElseThrow();
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
