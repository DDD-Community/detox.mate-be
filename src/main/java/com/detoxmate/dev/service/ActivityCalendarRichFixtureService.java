package com.detoxmate.dev.service;

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
import com.detoxmate.dev.dto.ActivityCalendarRichFixtureResponse;
import com.detoxmate.dev.dto.FixtureCheckDatesResponse;
import com.detoxmate.dev.dto.FixtureSummaryResponse;
import com.detoxmate.dev.dto.FixtureUserResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Profile({"local", "dev", "test"})
@Service
@RequiredArgsConstructor
public class ActivityCalendarRichFixtureService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String FIXTURE_NAME = "activity-calendar-rich";
    private static final String INVITE_CODE = "ACR01";
    private static final String GROUP_NAME = "캘린더시드";
    private static final long TOTAL_USAGE_GOAL_TYPE_ID = 3L;

    private final GroupRepository groupRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupChallengeParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final UsageGoalTypeRepository usageGoalTypeRepository;
    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final ChallengeRecordStatusCountRepository statusCountRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Transactional
    public ActivityCalendarRichFixtureResponse seed() {
        deleteExistingFixture();

        LocalDate today = LocalDate.now(clock.withZone(KST));
        LocalDate firstVerificationDate = today.minusDays(8);
        LocalDate dMinus8 = today.minusDays(8);
        LocalDate dMinus7 = today.minusDays(7);
        LocalDate dMinus6 = today.minusDays(6);
        LocalDate dMinus5 = today.minusDays(5);
        LocalDate dMinus4 = today.minusDays(4);
        LocalDate dMinus3 = today.minusDays(3);
        LocalDate dMinus2 = today.minusDays(2);
        LocalDate dMinus1 = today.minusDays(1);
        UsageGoalType totalUsage = totalUsageGoalType();
        Group group = groupRepository.saveAndFlush(Group.createNew(GROUP_NAME, INVITE_CODE));
        GroupChallenge challenge = saveActiveChallenge(group, firstVerificationDate.minusDays(1).atStartOfDay());

        FixtureMember me = saveFixtureMember(group, challenge, "me", "캘린더 나", true, today.minusDays(10).atTime(10, 0));
        FixtureMember jisu = saveFixtureMember(group, challenge, "member", "캘린더 지수", false, dMinus8.atTime(10, 0));
        FixtureMember minjun = saveFixtureMember(group, challenge, "member", "캘린더 민준", false, dMinus6.atTime(10, 0));

        UserUsageGoalTime meGoal = saveGoal(me.user(), totalUsage, 120, today.minusDays(9).atTime(9, 0));
        UserUsageGoalTime jisuGoal = saveGoal(jisu.user(), totalUsage, 120, dMinus7.atTime(9, 0));
        UserUsageGoalTime minjunGoal = saveGoal(minjun.user(), totalUsage, 120, dMinus5.atTime(9, 0));

        certify(challenge, me, meGoal, dMinus8, LocalTime.of(20, 30), 90, true);
        certify(challenge, me, meGoal, dMinus7, LocalTime.of(20, 30), 90, true);

        certify(challenge, me, meGoal, dMinus6, LocalTime.of(20, 30), 90, true);
        certify(challenge, jisu, jisuGoal, dMinus6, LocalTime.of(20, 20), 80, true);

        certify(challenge, me, meGoal, dMinus5, LocalTime.of(20, 30), 90, true);

        certify(challenge, me, meGoal, dMinus4, LocalTime.of(20, 30), 90, true);
        certify(challenge, jisu, jisuGoal, dMinus4, LocalTime.of(20, 20), 80, true);
        certify(challenge, minjun, minjunGoal, dMinus4, LocalTime.of(20, 10), 100, true);

        certify(challenge, jisu, jisuGoal, dMinus3, LocalTime.of(20, 30), 80, true);
        certify(challenge, minjun, minjunGoal, dMinus3, LocalTime.of(20, 20), 100, true);

        certify(challenge, me, meGoal, dMinus2, LocalTime.of(20, 30), 90, true);
        certify(challenge, jisu, jisuGoal, dMinus2, LocalTime.of(20, 20), 80, true);
        certify(challenge, minjun, minjunGoal, dMinus2, LocalTime.of(20, 10), 100, true);

        certify(challenge, me, meGoal, dMinus1, LocalTime.of(20, 30), 90, true);
        certify(challenge, jisu, jisuGoal, dMinus1, LocalTime.of(20, 20), 80, true);

        return new ActivityCalendarRichFixtureResponse(
                FIXTURE_NAME,
                group.getId(),
                challenge.getId(),
                INVITE_CODE,
                today,
                firstVerificationDate,
                new FixtureSummaryResponse(5, 3, 0, 8),
                new FixtureCheckDatesResponse(dMinus8, dMinus5, today),
                List.of(toUserResponse(me), toUserResponse(jisu), toUserResponse(minjun))
        );
    }

    private UsageGoalType totalUsageGoalType() {
        return usageGoalTypeRepository.findByCode(UsageGoalTypeCode.TOTAL_USAGE)
                .orElseGet(() -> usageGoalTypeRepository.saveAndFlush(
                        UsageGoalType.create(nextTotalUsageGoalTypeId(), UsageGoalTypeCode.TOTAL_USAGE)
                ));
    }

    private Long nextTotalUsageGoalTypeId() {
        if (usageGoalTypeRepository.findById(TOTAL_USAGE_GOAL_TYPE_ID).isEmpty()) {
            return TOTAL_USAGE_GOAL_TYPE_ID;
        }

        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(usage_goal_type_id), 0) + 1 FROM usage_goal_type",
                Long.class
        );
    }

    private GroupChallenge saveActiveChallenge(Group group, LocalDateTime startAt) {
        GroupChallenge challenge = GroupChallenge.createFirst(group.getId());
        challenge.activate(startAt);
        return groupChallengeRepository.saveAndFlush(challenge);
    }

    private FixtureMember saveFixtureMember(
            Group group,
            GroupChallenge challenge,
            String role,
            String displayName,
            boolean owner,
            LocalDateTime joinedAt
    ) {
        User user = userRepository.saveAndFlush(User.createNew(displayName, "profiles/fixtures/" + displayName + ".png"));
        GroupMember groupMember = owner
                ? GroupMember.createOwner(user.getId(), group.getId())
                : GroupMember.createMember(user.getId(), group.getId());
        GroupMember savedGroupMember = groupMemberRepository.saveAndFlush(groupMember);
        updateTimestamp("group_members", "group_member_id", savedGroupMember.getId(), "joined_at", joinedAt);

        GroupChallengeParticipant participant = participantRepository.saveAndFlush(
                GroupChallengeParticipant.join(savedGroupMember.getId(), challenge.getId())
        );
        updateTimestamp("group_challenge_participants", "group_challenge_participant_id", participant.getId(), "joined_at", joinedAt);

        return new FixtureMember(role, user, participant);
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
        updateCreatedAndUpdatedAt(
                "user_usage_goal_times",
                "user_usage_goal_times_id",
                goal.getId(),
                setAt
        );
        return goal;
    }

    private void certify(
            GroupChallenge challenge,
            FixtureMember member,
            UserUsageGoalTime goal,
            LocalDate recordDate,
            LocalTime submittedTime,
            int usedMinutes,
            boolean achieved
    ) {
        ChallengeRecord challengeRecord = challengeRecordRepository.saveAndFlush(
                ChallengeRecord.create(challenge.getId(), member.participant().getId(), recordDate)
        );
        ActivityRecord activityRecord = ActivityRecord.create(
                member.user(),
                member.participant(),
                "activity-records/fixtures/" + member.user().getId() + "/" + recordDate + ".png",
                achieved ? "오늘도 목표 시간 안에서 잘 버텼어요." : "오늘은 목표를 넘겼지만 인증은 완료했어요."
        );
        activityRecord.addDetail(goal, usedMinutes, achieved);
        ActivityRecord savedActivityRecord = activityRecordRepository.saveAndFlush(activityRecord);
        LocalDateTime submittedAt = recordDate.atTime(submittedTime);
        jdbcTemplate.update(
                "UPDATE activity_record SET created_at = ? WHERE activity_record_id = ?",
                Timestamp.valueOf(submittedAt),
                savedActivityRecord.getId()
        );

        challengeRecord.certify(
                savedActivityRecord.getId(),
                member.participant().getId(),
                achieved ? ChallengeRecordCertificationResult.SUCCESS : ChallengeRecordCertificationResult.FAIL
        );
        challengeRecordRepository.saveAndFlush(challengeRecord);
        statusCountRepository.saveAndFlush(ChallengeRecordStatusCount.create(challengeRecord.getId()));
    }

    private FixtureUserResponse toUserResponse(FixtureMember member) {
        return new FixtureUserResponse(
                member.role(),
                member.user().getId(),
                member.user().getDisplayName(),
                jwtTokenProvider.createAccessToken(member.user().getId())
        );
    }

    private void deleteExistingFixture() {
        Optional<Group> fixtureGroup = groupRepository.findByInviteCode(INVITE_CODE);
        if (fixtureGroup.isEmpty()) {
            return;
        }

        Long groupId = fixtureGroup.get().getId();
        List<Long> groupMemberIds = ids("SELECT group_member_id FROM group_members WHERE group_id = ?", groupId);
        List<Long> userIds = ids("SELECT user_id FROM group_members WHERE group_id = ?", groupId);
        List<Long> challengeIds = ids("SELECT group_challenge_id FROM group_challenges WHERE group_id = ?", groupId);
        List<Long> participantIds = idsByIn(
                "SELECT group_challenge_participant_id FROM group_challenge_participants WHERE group_challenge_id",
                challengeIds
        );
        List<Long> challengeRecordIds = idsByIn(
                "SELECT challenge_record_id FROM challenge_record WHERE group_challenge_id",
                challengeIds
        );
        List<Long> activityRecordIds = idsByIn(
                "SELECT activity_record_id FROM activity_record WHERE group_challenge_participant_id",
                participantIds
        );

        deleteIn("comments", "challenge_record_id", challengeRecordIds);
        deleteIn("reactions", "challenge_record_id", challengeRecordIds);
        deleteIn("pokes", "challenge_record_id", challengeRecordIds);
        deleteIn("challenge_record_status", "challenge_record_id", challengeRecordIds);
        deleteIn("activity_record_detail", "activity_record_id", activityRecordIds);
        deleteIn("challenge_record", "challenge_record_id", challengeRecordIds);
        deleteIn("activity_record", "activity_record_id", activityRecordIds);
        deleteIn("user_usage_goal_times", "user_id", userIds);
        deleteIn("group_challenge_participants", "group_challenge_participant_id", participantIds);
        deleteIn("group_challenges", "group_challenge_id", challengeIds);
        deleteIn("group_members", "group_member_id", groupMemberIds);
        deleteIn("social_login_users", "user_id", userIds);
        deleteIn("refresh_token_session", "user_id", userIds);
        deleteIn("users", "user_id", userIds);
        groupRepository.delete(fixtureGroup.get());
        groupRepository.flush();
    }

    private List<Long> ids(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
    }

    private List<Long> idsByIn(String sqlPrefix, List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        String placeholders = placeholders(ids);
        return ids(sqlPrefix + " IN (" + placeholders + ")", ids.toArray());
    }

    private void deleteIn(String tableName, String columnName, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }

        jdbcTemplate.update(
                "DELETE FROM " + tableName + " WHERE " + columnName + " IN (" + placeholders(ids) + ")",
                ids.toArray()
        );
    }

    private String placeholders(Collection<Long> ids) {
        return String.join(",", ids.stream().map(id -> "?").toList());
    }

    private void updateTimestamp(
            String tableName,
            String idColumnName,
            Long id,
            String timestampColumnName,
            LocalDateTime timestamp
    ) {
        jdbcTemplate.update(
                "UPDATE " + tableName + " SET " + timestampColumnName + " = ? WHERE " + idColumnName + " = ?",
                Timestamp.valueOf(timestamp),
                id
        );
    }

    private void updateCreatedAndUpdatedAt(String tableName, String idColumnName, Long id, LocalDateTime timestamp) {
        jdbcTemplate.update(
                "UPDATE " + tableName + " SET created_at = ?, updated_at = ? WHERE " + idColumnName + " = ?",
                Timestamp.valueOf(timestamp),
                Timestamp.valueOf(timestamp),
                id
        );
    }

    private record FixtureMember(String role, User user, GroupChallengeParticipant participant) {
    }
}
