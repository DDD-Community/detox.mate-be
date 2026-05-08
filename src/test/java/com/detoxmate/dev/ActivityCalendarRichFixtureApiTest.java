package com.detoxmate.dev;

import com.detoxmate.dev.dto.ActivityCalendarRichFixtureResponse;
import com.detoxmate.dev.service.ActivityCalendarRichFixtureService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActivityCalendarRichFixtureApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ActivityCalendarRichFixtureService fixtureService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("activity-calendar-rich fixture를 생성하면 응답 토큰으로 캘린더 happy case를 조회할 수 있다")
    void seedActivityCalendarRichFixture() throws Exception {
        JsonNode fixture = createFixture();
        long groupId = fixture.get("groupId").asLong();
        String accessToken = fixture.get("users").get(0).get("accessToken").asText();

        mockMvc.perform(get("/groups/{groupId}/activity-calendar", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId))
                .andExpect(jsonPath("$.firstVerificationDate").value(fixture.get("firstVerificationDate").asText()))
                .andExpect(jsonPath("$.streakDays").value(8))
                .andExpect(jsonPath("$.summary.allCount").value(4))
                .andExpect(jsonPath("$.summary.halfCount").value(4))
                .andExpect(jsonPath("$.summary.resetCount").value(0));

        mockMvc.perform(get(
                        "/groups/{groupId}/activity-calendar/days/{date}",
                        groupId,
                        fixture.get("checkDates").get("halfDay").asText()
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailySummary.result").value("HALF"))
                .andExpect(jsonPath("$.dailySummary.activeMemberCount").value(3))
                .andExpect(jsonPath("$.dailySummary.certifiedMemberCount").value(2))
                .andExpect(jsonPath("$.dailySummary.requiredCount").value(2))
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
    }

    @Test
    @DisplayName("activity-calendar-rich fixture는 같은 초대코드 그룹을 삭제하고 재생성한다")
    void seedActivityCalendarRichFixtureIsIdempotent() {
        ActivityCalendarRichFixtureResponse first = fixtureService.seed();
        ActivityCalendarRichFixtureResponse second = fixtureService.seed();

        assertThat(second.inviteCode()).isEqualTo("ACR01");
        assertThat(second.groupId()).isNotEqualTo(first.groupId());
    }

    private JsonNode createFixture() throws Exception {
        String response = mockMvc.perform(post("/dev/fixtures/activity-calendar-rich"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixture").value("activity-calendar-rich"))
                .andExpect(jsonPath("$.inviteCode").value("ACR01"))
                .andExpect(jsonPath("$.summary.allCount").value(4))
                .andExpect(jsonPath("$.summary.halfCount").value(4))
                .andExpect(jsonPath("$.summary.resetCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }
}
