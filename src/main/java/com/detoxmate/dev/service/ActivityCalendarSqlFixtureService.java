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
    private static final long ME_USER_ID = -910000001L;
    private static final long JISOO_USER_ID = -910000002L;
    private static final long MINJUN_USER_ID = -910000003L;

    private final JdbcTemplate jdbcTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    @Transactional
    public ActivityCalendarRichFixtureResponse reset() {
        FixtureDates dates = FixtureDates.from(LocalDate.now(clock.withZone(KST)));
        Map<String, String> tokens = tokens(dates);
        tokens.put("__GROUPS_TABLE__", groupsTableName());

        executeSql("delete.sql", tokens);
        executeSql("seed.sql", tokens);

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
                        user("me", ME_USER_ID),
                        user("member", JISOO_USER_ID),
                        user("member", MINJUN_USER_ID)
                )
        );
    }

    private FixtureUserResponse user(String role, Long userId) {
        String displayName = jdbcTemplate.queryForObject(
                "SELECT display_name FROM users WHERE user_id = ?",
                String.class,
                userId
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

    private Map<String, String> tokens(FixtureDates dates) {
        Map<String, String> tokens = new LinkedHashMap<>();
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
}
