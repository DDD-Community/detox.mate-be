package com.detoxmate.group;

import com.detoxmate.user.controller.DevAuthController;
import com.detoxmate.user.service.AuthService;
import com.detoxmate.user.service.DevAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({
        GroupMemberProfileEndToEndHttpApiTest.MutableClockConfig.class,
        GroupMemberProfileEndToEndHttpApiTest.DevAuthTestConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GroupMemberProfileEndToEndHttpApiTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 16);

    @DynamicPropertySource
    static void useIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:group-member-profile-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
    }

    @LocalServerPort
    int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("그룹 멤버 마이페이지를 실제 HTTP API로 본인/타인/목표 미설정 케이스까지 조회한다")
    void groupMemberProfileFlow_matchesThroughHttpApis() throws Exception {
        JsonNode fixture = postJson("/dev/fixtures/activity-calendar-rich", null, null, 200);
        long groupId = fixture.get("groupId").asLong();
        String meBearer = bearer(fixture.get("users").get(0).get("accessToken").asText());

        JsonNode myGroup = getJson("/me/groups", meBearer).get(0);
        long meGroupMemberId = memberByDisplayName(myGroup, "캘린더 나").get("id").asLong();
        long jisooGroupMemberId = memberByDisplayName(myGroup, "캘린더 지수").get("id").asLong();

        JsonNode meProfile = getJson("/groups/" + groupId + "/members/" + meGroupMemberId, meBearer);
        assertThat(meProfile.get("groupMemberId").asLong()).isEqualTo(meGroupMemberId);
        assertThat(meProfile.get("memberStatus").asText()).isEqualTo("ACTIVE");
        assertThat(meProfile.get("goalStatus").asText()).isEqualTo("SET");
        assertThat(meProfile.has("id")).isFalse();
        assertThat(meProfile.has("status")).isFalse();
        assertThat(meProfile.has("stats")).isFalse();
        assertThat(meProfile.at("/currentGoals/0/usageGoalType").asText()).isEqualTo("TOTAL_USAGE");
        assertThat(meProfile.at("/currentGoals/0/goalMinutes").asInt()).isEqualTo(120);
        assertThat(meProfile.at("/currentGoals/0/createdAt").asText()).isEqualTo("2026-04-07T09:00:00");
        assertThat(meProfile.at("/currentGoals/0/setAt").isMissingNode()).isTrue();
        assertThat(meProfile.at("/goalChangeAvailability/canChange").asBoolean()).isFalse();
        assertThat(meProfile.at("/goalChangeAvailability/nextChangeAvailableDate").asText()).isEqualTo("2026-04-21");
        assertThat(meProfile.at("/goalChangeAvailability/remainingDays").asInt()).isEqualTo(5);
        assertThat(meProfile.at("/activitySummary/firstCertifiedDate").asText()).isEqualTo("2026-04-08");
        assertThat(meProfile.at("/activitySummary/dayCount").asInt()).isEqualTo(9);
        assertThat(meProfile.at("/activitySummary/achievementRate").asInt()).isEqualTo(78);
        assertThat(meProfile.at("/weeklySummary/startDate").asText()).isEqualTo("2026-04-10");
        assertThat(meProfile.at("/weeklySummary/endDate").asText()).isEqualTo("2026-04-16");
        assertThat(meProfile.at("/weeklySummary/totalDays").asInt()).isEqualTo(7);
        assertThat(meProfile.at("/weeklySummary/averageUsedMinutes").asInt()).isEqualTo(64);
        assertThat(meProfile.at("/weeklySummary/goalMinutes").asInt()).isEqualTo(120);
        assertThat(meProfile.at("/weeklySummary/differenceMinutes").asInt()).isEqualTo(56);
        assertThat(meProfile.at("/weeklySummary/certifiedDays").asInt()).isEqualTo(5);
        assertThat(meProfile.at("/weeklySummary/achievedDays").asInt()).isEqualTo(5);

        JsonNode jisooProfile = getJson("/groups/" + groupId + "/members/" + jisooGroupMemberId, meBearer);
        assertThat(jisooProfile.get("groupMemberId").asLong()).isEqualTo(jisooGroupMemberId);
        assertThat(jisooProfile.get("goalChangeAvailability").isNull()).isTrue();
        assertThat(jisooProfile.at("/activitySummary/firstCertifiedDate").asText()).isEqualTo("2026-04-10");
        assertThat(jisooProfile.at("/activitySummary/dayCount").asInt()).isEqualTo(7);
        assertThat(jisooProfile.at("/activitySummary/achievementRate").asInt()).isEqualTo(71);
        assertThat(jisooProfile.at("/weeklySummary/averageUsedMinutes").asInt()).isEqualTo(57);
        assertThat(jisooProfile.at("/weeklySummary/certifiedDays").asInt()).isEqualTo(5);
        assertThat(jisooProfile.at("/weeklySummary/achievedDays").asInt()).isEqualTo(5);

        JsonNode login = postJson(
                "/dev/auth/test-login",
                null,
                """
                        { "testUserKey": "front-c" }
                        """,
                200
        );
        String newUserBearer = bearer(login.get("accessToken").asText());
        JsonNode createdGroup = postJson(
                "/groups",
                newUserBearer,
                """
                        { "name": "목표없음" }
                        """,
                201
        );
        long emptyGroupId = createdGroup.get("id").asLong();
        long emptyGroupMemberId = createdGroup.get("members").get(0).get("id").asLong();
        JsonNode emptyProfile = getJson("/groups/" + emptyGroupId + "/members/" + emptyGroupMemberId, newUserBearer);
        assertThat(emptyProfile.get("goalStatus").asText()).isEqualTo("NOT_SET");
        assertThat(emptyProfile.get("currentGoals")).isEmpty();
        assertThat(emptyProfile.at("/goalChangeAvailability/canChange").asBoolean()).isTrue();
        assertThat(emptyProfile.at("/goalChangeAvailability/nextChangeAvailableDate").isNull()).isTrue();
        assertThat(emptyProfile.at("/goalChangeAvailability/remainingDays").asInt()).isZero();
        assertThat(emptyProfile.at("/activitySummary/firstCertifiedDate").isNull()).isTrue();
        assertThat(emptyProfile.at("/activitySummary/dayCount").asInt()).isZero();
        assertThat(emptyProfile.at("/activitySummary/achievementRate").asInt()).isZero();
        assertThat(emptyProfile.at("/weeklySummary/averageUsedMinutes").asInt()).isZero();
        assertThat(emptyProfile.at("/weeklySummary/goalMinutes").isNull()).isTrue();
        assertThat(emptyProfile.at("/weeklySummary/differenceMinutes").isNull()).isTrue();

    }

    private JsonNode getJson(String path, String bearer) throws Exception {
        HttpResponse<String> response = send("GET", path, bearer, null);
        assertThat(response.statusCode()).as("GET " + path + " -> " + response.body()).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    private JsonNode postJson(String path, String bearer, String body, int expectedStatus) throws Exception {
        HttpResponse<String> response = send("POST", path, bearer, body);
        assertThat(response.statusCode()).as("POST " + path + " -> " + response.body()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> send(String method, String path, String bearer, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));

        if (bearer != null) {
            builder.header("Authorization", bearer);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private JsonNode memberByDisplayName(JsonNode group, String displayName) {
        for (JsonNode member : group.get("members")) {
            if (displayName.equals(member.get("displayName").asText())) {
                return member;
            }
        }
        throw new AssertionError("member displayName=" + displayName + " not found: " + group);
    }

    static class MutableClock extends Clock {

        private final Instant instant;

        MutableClock(LocalDate date) {
            this.instant = date.atTime(9, 0).atZone(KST).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return KST;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(TODAY);
        }
    }

    @TestConfiguration
    static class DevAuthTestConfig {

        @Bean
        DevAuthService devAuthService(AuthService authService) {
            return new DevAuthService(authService);
        }

        @Bean
        DevAuthController devAuthController(DevAuthService devAuthService) {
            return new DevAuthController(devAuthService);
        }
    }
}
