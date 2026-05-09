package com.detoxmate.dev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActivityCalendarSqlFixtureApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("SQL reset fixture를 생성하면 응답 토큰으로 캘린더 happy case를 조회할 수 있다")
    void resetActivityCalendarSqlFixture() throws Exception {
        JsonNode fixture = resetFixture();
        long groupId = fixture.get("groupId").asLong();
        String accessToken = fixture.get("users").get(0).get("accessToken").asText();

        mockMvc.perform(get("/groups/{groupId}/activity-calendar", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId))
                .andExpect(jsonPath("$.firstVerificationDate").value(fixture.get("firstVerificationDate").asText()))
                .andExpect(jsonPath("$.streakDays").value(8))
                .andExpect(jsonPath("$.summary.allCount").value(5))
                .andExpect(jsonPath("$.summary.halfCount").value(3))
                .andExpect(jsonPath("$.summary.resetCount").value(0));

        mockMvc.perform(get(
                        "/groups/{groupId}/activity-calendar/days/{date}",
                        groupId,
                        fixture.get("checkDates").get("halfDay").asText()
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailySummary.result").value("HALF"))
                .andExpect(jsonPath("$.dailySummary.activeMemberCount").value(2))
                .andExpect(jsonPath("$.dailySummary.certifiedMemberCount").value(1))
                .andExpect(jsonPath("$.dailySummary.requiredCount").value(1))
                .andExpect(jsonPath("$.members.length()").value(3));

        mockMvc.perform(get(
                                "/groups/{groupId}/activity-calendar/days/{date}",
                                groupId,
                                fixture.get("checkDates").get("allDay").asText()
                        )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailySummary.result").value("ALL"))
                .andExpect(jsonPath("$.dailySummary.activeMemberCount").value(1))
                .andExpect(jsonPath("$.dailySummary.certifiedMemberCount").value(1))
                .andExpect(jsonPath("$.members.length()").value(3));

        mockMvc.perform(get(
                        "/groups/{groupId}/activity-calendar/days/{date}",
                        groupId,
                        fixture.get("today").asText()
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailySummary.dayStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.dailySummary.result").doesNotExist())
                .andExpect(jsonPath("$.dailySummary.activeMemberCount").value(3));

        assertStaggeredFixtureDates(groupId, fixture);
    }

    @Test
    @DisplayName("SQL reset fixture는 여러 번 호출해도 같은 fixture 상태로 재생성된다")
    void resetActivityCalendarSqlFixtureIsIdempotent() throws Exception {
        JsonNode first = resetFixture();
        JsonNode second = resetFixture();

        assertThat(second.get("inviteCode").asText()).isEqualTo("ACR01");
        assertThat(second.get("groupId").asLong()).isEqualTo(first.get("groupId").asLong());
        assertThat(second.get("summary").get("streakDays").asInt()).isEqualTo(8);
    }

    private JsonNode resetFixture() throws Exception {
        String response = mockMvc.perform(post("/dev/fixtures/activity-calendar-rich/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixture").value("activity-calendar-rich"))
                .andExpect(jsonPath("$.inviteCode").value("ACR01"))
                .andExpect(jsonPath("$.summary.allCount").value(5))
                .andExpect(jsonPath("$.summary.halfCount").value(3))
                .andExpect(jsonPath("$.summary.resetCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private void assertStaggeredFixtureDates(long groupId, JsonNode fixture) {
        LocalDate today = LocalDate.parse(fixture.get("today").asText());

        assertFixtureMemberDates(groupId, "캘린더 나", today.minusDays(10), today.minusDays(9), today.minusDays(8));
        assertFixtureMemberDates(groupId, "캘린더 지수", today.minusDays(8), today.minusDays(7), today.minusDays(6));
        assertFixtureMemberDates(groupId, "캘린더 민준", today.minusDays(6), today.minusDays(5), today.minusDays(4));
    }

    private void assertFixtureMemberDates(
            long groupId,
            String displayName,
            LocalDate joinedDate,
            LocalDate goalSetDate,
            LocalDate firstCertifiedDate
    ) {
        assertThat(queryDate("""
                SELECT gm.joined_at
                FROM group_members gm
                JOIN users u ON u.user_id = gm.user_id
                WHERE gm.group_id = ? AND u.display_name = ?
                """, groupId, displayName)).isEqualTo(joinedDate);
        assertThat(queryDate("""
                SELECT uugt.created_at
                FROM user_usage_goal_times uugt
                JOIN users u ON u.user_id = uugt.user_id
                JOIN group_members gm ON gm.user_id = u.user_id
                WHERE gm.group_id = ? AND u.display_name = ?
                """, groupId, displayName)).isEqualTo(goalSetDate);
        assertThat(queryDate("""
                SELECT MIN(ar.created_at)
                FROM activity_record ar
                JOIN users u ON u.user_id = ar.user_id
                JOIN group_members gm ON gm.user_id = u.user_id
                WHERE gm.group_id = ? AND u.display_name = ?
                """, groupId, displayName)).isEqualTo(firstCertifiedDate);
    }

    private LocalDate queryDate(String sql, Object... args) {
        Timestamp timestamp = jdbcTemplate.queryForObject(sql, Timestamp.class, args);
        return timestamp.toLocalDateTime().toLocalDate();
    }
}
