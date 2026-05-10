package com.detoxmate.dev.service;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.dev.dto.ActivityCalendarRichFixtureResponse;
import com.detoxmate.dev.dto.FixtureCheckDatesResponse;
import com.detoxmate.dev.dto.FixtureSummaryResponse;
import com.detoxmate.dev.dto.FixtureUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Profile({"local", "dev", "test"})
@Service
@RequiredArgsConstructor
public class ActivityCalendarSqlFixtureService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter SQL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FIXTURE_NAME = "activity-calendar-rich";
    private static final String FIXTURE_PATH = "dev-fixtures/activity-calendar-rich/";
    private static final String INVITE_CODE = "ACR01";

    private final JdbcTemplate jdbcTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    @Transactional
    public synchronized ActivityCalendarRichFixtureResponse reset() {
        FixtureDates dates = FixtureDates.from(LocalDate.now(clock.withZone(KST)));
        Map<String, String> tokens = tokens(dates);
        tokens.put("__GROUPS_TABLE__", groupsTableName());

        executeSql("delete.sql", tokens);
        executeSql("seed.sql", tokens);
        advanceIdentityColumns();

        return response(dates);
    }

    private void executeSql(String filename, Map<String, String> tokens) {
        String sql = render(filename, tokens);
        ByteArrayResource resource = new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8), filename);
        jdbcTemplate.execute((Connection connection) -> {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(resource, StandardCharsets.UTF_8));
            return null;
        });
    }

    private String render(String filename, Map<String, String> tokens) {
        ClassPathResource resource = new ClassPathResource(FIXTURE_PATH + filename);
        try {
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> token : tokens.entrySet()) {
                sql = sql.replace(token.getKey(), token.getValue());
            }
            return sql;
        } catch (IOException exception) {
            throw new IllegalStateException("Activity calendar fixture SQL을 읽을 수 없습니다: " + filename, exception);
        }
    }

    private ActivityCalendarRichFixtureResponse response(FixtureDates dates) {
        Long groupId = jdbcTemplate.queryForObject(
                "SELECT group_id FROM " + groupsTableName() + " WHERE invite_code = ?",
                Long.class,
                INVITE_CODE
        );
        Long challengeId = jdbcTemplate.queryForObject(
                "SELECT group_challenge_id FROM group_challenges WHERE group_id = ? ORDER BY challenge_no DESC LIMIT 1",
                Long.class,
                groupId
        );

        return new ActivityCalendarRichFixtureResponse(
                FIXTURE_NAME,
                groupId,
                challengeId,
                INVITE_CODE,
                dates.today(),
                dates.dMinus8(),
                new FixtureSummaryResponse(5, 3, 0, 8),
                new FixtureCheckDatesResponse(dates.dMinus8(), dates.dMinus5(), dates.today()),
                List.of(
                        user("me", groupId, "캘린더 나"),
                        user("member", groupId, "캘린더 지수"),
                        user("member", groupId, "캘린더 민준")
                )
        );
    }

    private FixtureUserResponse user(String role, Long groupId, String displayName) {
        Long userId = jdbcTemplate.queryForObject(
                """
                        SELECT u.user_id
                        FROM users u
                        JOIN group_members gm ON gm.user_id = u.user_id
                        WHERE gm.group_id = ? AND u.display_name = ?
                        """,
                Long.class,
                groupId,
                displayName
        );
        return new FixtureUserResponse(role, userId, displayName, jwtTokenProvider.createAccessToken(userId));
    }

    private String groupsTableName() {
        String productName = jdbcTemplate.execute((Connection connection) -> {
            try {
                return connection.getMetaData().getDatabaseProductName();
            } catch (SQLException exception) {
                throw new IllegalStateException("DB product name을 확인할 수 없습니다.", exception);
            }
        });
        if (productName != null && productName.contains("H2")) {
            return "\"groups\"";
        }
        return "`groups`";
    }

    private void advanceIdentityColumns() {
        advanceIdentityColumn("users", "user_id");
        advanceIdentityColumn(groupsTableName(), "group_id");
        advanceIdentityColumn("group_challenges", "group_challenge_id");
        advanceIdentityColumn("group_members", "group_member_id");
        advanceIdentityColumn("group_challenge_participants", "group_challenge_participant_id");
        advanceIdentityColumn("user_usage_goal_times", "user_usage_goal_times_id");
        advanceIdentityColumn("activity_record", "activity_record_id");
        advanceIdentityColumn("activity_record_detail", "activity_record_detail_id");
        advanceIdentityColumn("challenge_record", "challenge_record_id");
        advanceIdentityColumn("challenge_record_status", "challenge_record_status_id");
    }

    private void advanceIdentityColumn(String tableName, String columnName) {
        long nextId = nextId(tableName, columnName);
        String productName = databaseProductName();
        if (productName != null && productName.contains("H2")) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " RESTART WITH " + nextId);
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " AUTO_INCREMENT = " + nextId);
    }

    private long nextId(String tableName, String columnName) {
        Long nextId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(" + columnName + "), 0) + 1 FROM " + tableName,
                Long.class
        );
        if (nextId == null) {
            throw new IllegalStateException("fixture ID를 할당할 수 없습니다: " + tableName + "." + columnName);
        }
        return nextId;
    }

    private String databaseProductName() {
        return jdbcTemplate.execute((Connection connection) -> {
            try {
                return connection.getMetaData().getDatabaseProductName();
            } catch (SQLException exception) {
                throw new IllegalStateException("DB product name을 확인할 수 없습니다.", exception);
            }
        });
    }

    private Map<String, String> tokens(FixtureDates dates) {
        FixtureIds ids = FixtureIds.next(jdbcTemplate, groupsTableName());
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("__TOTAL_USAGE_GOAL_TYPE_ID__", String.valueOf(ids.totalUsageGoalTypeId()));
        tokens.put("__ME_USER_ID__", String.valueOf(ids.meUserId()));
        tokens.put("__JISOO_USER_ID__", String.valueOf(ids.jisooUserId()));
        tokens.put("__MINJUN_USER_ID__", String.valueOf(ids.minjunUserId()));
        tokens.put("__GROUP_ID__", String.valueOf(ids.groupId()));
        tokens.put("__GROUP_CHALLENGE_ID__", String.valueOf(ids.groupChallengeId()));
        tokens.put("__ME_GROUP_MEMBER_ID__", String.valueOf(ids.meGroupMemberId()));
        tokens.put("__JISOO_GROUP_MEMBER_ID__", String.valueOf(ids.jisooGroupMemberId()));
        tokens.put("__MINJUN_GROUP_MEMBER_ID__", String.valueOf(ids.minjunGroupMemberId()));
        tokens.put("__ME_PARTICIPANT_ID__", String.valueOf(ids.meParticipantId()));
        tokens.put("__JISOO_PARTICIPANT_ID__", String.valueOf(ids.jisooParticipantId()));
        tokens.put("__MINJUN_PARTICIPANT_ID__", String.valueOf(ids.minjunParticipantId()));
        tokens.put("__ME_GOAL_ID__", String.valueOf(ids.meGoalId()));
        tokens.put("__JISOO_GOAL_ID__", String.valueOf(ids.jisooGoalId()));
        tokens.put("__MINJUN_GOAL_ID__", String.valueOf(ids.minjunGoalId()));
        tokens.put("__ME_JOINED_AT__", dateTime(dates.dMinus10(), LocalTime.of(10, 0)));
        tokens.put("__JISOO_JOINED_AT__", dateTime(dates.dMinus8(), LocalTime.of(10, 0)));
        tokens.put("__MINJUN_JOINED_AT__", dateTime(dates.dMinus6(), LocalTime.of(10, 0)));
        tokens.put("__ME_GOAL_SET_AT__", dateTime(dates.dMinus9(), LocalTime.of(9, 0)));
        tokens.put("__JISOO_GOAL_SET_AT__", dateTime(dates.dMinus7(), LocalTime.of(9, 0)));
        tokens.put("__MINJUN_GOAL_SET_AT__", dateTime(dates.dMinus5(), LocalTime.of(9, 0)));
        tokens.put("__FIRST_VERIFICATION_AT__", dateTime(dates.dMinus8(), LocalTime.MIDNIGHT));

        for (int daysAgo = 1; daysAgo <= 8; daysAgo++) {
            LocalDate date = dates.today().minusDays(daysAgo);
            tokens.put("__D_MINUS_" + daysAgo + "__", date(date));
            tokens.put("__D_MINUS_" + daysAgo + "_ME_AT__", dateTime(date, LocalTime.of(20, 30)));
            tokens.put("__D_MINUS_" + daysAgo + "_JISOO_AT__", dateTime(date, LocalTime.of(20, 20)));
            tokens.put("__D_MINUS_" + daysAgo + "_MINJUN_AT__", dateTime(date, LocalTime.of(20, 10)));
        }
        for (int index = 1; index <= 15; index++) {
            tokens.put("__ACTIVITY_RECORD_" + index + "_ID__", String.valueOf(ids.activityRecordId(index)));
            tokens.put("__CHALLENGE_RECORD_" + index + "_ID__", String.valueOf(ids.challengeRecordId(index)));
            tokens.put("__ACTIVITY_RECORD_DETAIL_" + index + "_ID__", String.valueOf(ids.activityRecordDetailId(index)));
            tokens.put("__CHALLENGE_RECORD_STATUS_" + index + "_ID__", String.valueOf(ids.challengeRecordStatusId(index)));
        }

        return tokens;
    }

    private String date(LocalDate date) {
        return date.toString();
    }

    private String dateTime(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).format(SQL_DATE_TIME);
    }

    private record FixtureDates(
            LocalDate today,
            LocalDate dMinus10,
            LocalDate dMinus9,
            LocalDate dMinus8,
            LocalDate dMinus7,
            LocalDate dMinus6,
            LocalDate dMinus5
    ) {
        private static FixtureDates from(LocalDate today) {
            return new FixtureDates(
                    today,
                    today.minusDays(10),
                    today.minusDays(9),
                    today.minusDays(8),
                    today.minusDays(7),
                    today.minusDays(6),
                    today.minusDays(5)
            );
        }
    }

    private record FixtureIds(
            long totalUsageGoalTypeId,
            long meUserId,
            long jisooUserId,
            long minjunUserId,
            long groupId,
            long groupChallengeId,
            long meGroupMemberId,
            long jisooGroupMemberId,
            long minjunGroupMemberId,
            long meParticipantId,
            long jisooParticipantId,
            long minjunParticipantId,
            long meGoalId,
            long jisooGoalId,
            long minjunGoalId,
            long activityRecordBaseId,
            long challengeRecordBaseId,
            long activityRecordDetailBaseId,
            long challengeRecordStatusBaseId
    ) {
        private static FixtureIds next(JdbcTemplate jdbcTemplate, String groupsTableName) {
            long userId = nextId(jdbcTemplate, "users", "user_id");
            long groupMemberId = nextId(jdbcTemplate, "group_members", "group_member_id");
            long participantId = nextId(jdbcTemplate, "group_challenge_participants", "group_challenge_participant_id");
            long goalId = nextId(jdbcTemplate, "user_usage_goal_times", "user_usage_goal_times_id");

            return new FixtureIds(
                    totalUsageGoalTypeId(jdbcTemplate),
                    userId,
                    userId + 1,
                    userId + 2,
                    nextId(jdbcTemplate, groupsTableName, "group_id"),
                    nextId(jdbcTemplate, "group_challenges", "group_challenge_id"),
                    groupMemberId,
                    groupMemberId + 1,
                    groupMemberId + 2,
                    participantId,
                    participantId + 1,
                    participantId + 2,
                    goalId,
                    goalId + 1,
                    goalId + 2,
                    nextId(jdbcTemplate, "activity_record", "activity_record_id"),
                    nextId(jdbcTemplate, "challenge_record", "challenge_record_id"),
                    nextId(jdbcTemplate, "activity_record_detail", "activity_record_detail_id"),
                    nextId(jdbcTemplate, "challenge_record_status", "challenge_record_status_id")
            );
        }

        private static long totalUsageGoalTypeId(JdbcTemplate jdbcTemplate) {
            List<Long> existingIds = jdbcTemplate.query(
                    "SELECT usage_goal_type_id FROM usage_goal_type WHERE description = 'TOTAL_USAGE' ORDER BY usage_goal_type_id LIMIT 1",
                    (rs, rowNum) -> rs.getLong(1)
            );
            if (!existingIds.isEmpty()) {
                return existingIds.get(0);
            }
            return nextId(jdbcTemplate, "usage_goal_type", "usage_goal_type_id");
        }

        private static long nextId(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
            Long nextId = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(" + columnName + "), 0) + 1 FROM " + tableName,
                    Long.class
            );
            if (nextId == null) {
                throw new IllegalStateException("fixture ID를 할당할 수 없습니다: " + tableName + "." + columnName);
            }
            return nextId;
        }

        private long activityRecordId(int index) {
            return activityRecordBaseId + index - 1;
        }

        private long challengeRecordId(int index) {
            return challengeRecordBaseId + index - 1;
        }

        private long activityRecordDetailId(int index) {
            return activityRecordDetailBaseId + index - 1;
        }

        private long challengeRecordStatusId(int index) {
            return challengeRecordStatusBaseId + index - 1;
        }
    }
}
