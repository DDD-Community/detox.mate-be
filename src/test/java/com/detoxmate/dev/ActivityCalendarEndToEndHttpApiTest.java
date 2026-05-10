package com.detoxmate.dev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@Import(ActivityCalendarEndToEndHttpApiTest.MutableClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActivityCalendarEndToEndHttpApiTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 16);

    @DynamicPropertySource
    static void useIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:activity-calendar-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
    }

    @LocalServerPort
    int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    MutableClock clock;

    @Test
    @DisplayName("fixture 생성부터 활동 인증, 소셜 액션, 그룹 인증 계산, 스트릭 계산까지 실제 API로 검증한다")
    void activityCalendarFlow_matchesThroughHttpApis() throws Exception {
        clock.moveTo(TODAY);

        JsonNode fixture = postJson("/dev/fixtures/activity-calendar-rich/reset", null);
        long groupChallengeId = fixture.get("groupChallengeId").asLong();
        String meBearer = bearer(fixture.get("users").get(0).get("accessToken").asText());
        String memberBearer = bearer(fixture.get("users").get(1).get("accessToken").asText());

        JsonNode initialCalendar = getJson(
                "/group-challenges/" + groupChallengeId + "/activity-calendar",
                meBearer
        );
        assertThat(initialCalendar.get("firstVerificationDate").asText()).isEqualTo("2026-04-08");
        assertThat(initialCalendar.at("/summary/allCount").asInt()).isEqualTo(5);
        assertThat(initialCalendar.at("/summary/halfCount").asInt()).isEqualTo(3);
        assertThat(initialCalendar.at("/summary/resetCount").asInt()).isZero();
        assertThat(initialCalendar.get("streakDays").asInt()).isEqualTo(8);

        JsonNode todayFeed = getJson(
                "/group-challenges/" + groupChallengeId + "/challenge-records/today",
                meBearer
        );
        assertThat(todayFeed.at("/dailySummary/dayStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(todayFeed.at("/dailySummary/result").isMissingNode()
                || todayFeed.at("/dailySummary/result").isNull()).isTrue();
        assertThat(todayFeed.at("/dailySummary/activeMemberCount").asInt()).isEqualTo(3);

        JsonNode meTodayCard = meMember(todayFeed);
        JsonNode memberTodayCard = memberByDisplayName(todayFeed, "캘린더 지수");
        long meChallengeRecordId = meTodayCard.get("challengeRecordId").asLong();
        long meParticipantId = meTodayCard.get("groupChallengeParticipantId").asLong();
        long memberParticipantId = memberTodayCard.get("groupChallengeParticipantId").asLong();
        assertThat(meTodayCard.get("dailyStatus").asText()).isEqualTo("NOT_CERTIFIED");

        JsonNode beforeDetail = getJson(
                "/group-challenges/" + groupChallengeId + "/challenge-records/" + meChallengeRecordId,
                meBearer
        );
        assertThat(beforeDetail.get("dailyStatus").asText()).isEqualTo("NOT_CERTIFIED");
        assertThat(beforeDetail.get("challengeRecordId").asLong()).isEqualTo(meChallengeRecordId);
        assertThat(beforeDetail.get("activityRecord").isNull()).isTrue();
        assertThat(beforeDetail.at("/reactions/totalCount").asInt()).isZero();
        assertThat(beforeDetail.get("commentCount").asInt()).isZero();

        JsonNode createdComment = postJson(
                "/challenge-records/" + meChallengeRecordId + "/comments",
                meBearer,
                """
                        { "commentBody": "오늘 인증 같이 해요" }
                        """
        );
        assertThat(createdComment.get("challengeRecordId").asLong()).isEqualTo(meChallengeRecordId);
        assertThat(createdComment.get("commentBody").asText()).isEqualTo("오늘 인증 같이 해요");

        JsonNode comments = getJson(
                "/challenge-records/" + meChallengeRecordId + "/comments",
                meBearer
        );
        assertThat(comments.get("totalCount").asInt()).isEqualTo(1);
        assertThat(comments.at("/items/0/commentBody").asText()).isEqualTo("오늘 인증 같이 해요");

        JsonNode achievementCheck = postJson(
                "/activity-records/achievement-check",
                meBearer,
                """
                        {
                          "details": [
                            { "usageGoalType": "TOTAL_USAGE", "usedMinutes": 70 }
                          ]
                        }
                        """
        );
        assertThat(achievementCheck.get("allAchieved").asBoolean()).isTrue();
        assertThat(achievementCheck.at("/details/0/goalMinutes").asInt()).isEqualTo(120);
        assertThat(achievementCheck.at("/details/0/isAchieved").asBoolean()).isTrue();

        JsonNode createdActivity = postJson(
                "/activity-records",
                meBearer,
                """
                        {
                          "activityImageObjectKey": "activity-records/e2e/me-today.png",
                          "reflectionText": "오늘은 목표 안에서 마무리했어요",
                          "groupChallengeParticipantId": %d,
                          "details": [
                            { "usageGoalType": "TOTAL_USAGE", "usedMinutes": 70 }
                          ]
                        }
                        """.formatted(meParticipantId)
        );
        assertThat(createdActivity.get("groupChallengeParticipantId").asLong()).isEqualTo(meParticipantId);
        assertThat(createdActivity.get("allAchieved").asBoolean()).isTrue();

        JsonNode createdMemberActivity = postJson(
                "/activity-records",
                memberBearer,
                """
                        {
                          "activityImageObjectKey": "activity-records/e2e/member-today.png",
                          "reflectionText": "같이 인증했어요",
                          "groupChallengeParticipantId": %d,
                          "details": [
                            { "usageGoalType": "TOTAL_USAGE", "usedMinutes": 80 }
                          ]
                        }
                        """.formatted(memberParticipantId)
        );
        assertThat(createdMemberActivity.get("groupChallengeParticipantId").asLong()).isEqualTo(memberParticipantId);
        assertThat(createdMemberActivity.get("allAchieved").asBoolean()).isTrue();

        JsonNode certifiedTodayFeed = getJson(
                "/group-challenges/" + groupChallengeId + "/challenge-records/today",
                meBearer
        );
        JsonNode certifiedMeCard = meMember(certifiedTodayFeed);
        assertThat(certifiedMeCard.get("dailyStatus").asText()).isEqualTo("GOAL_ACHIEVED");
        assertThat(certifiedMeCard.get("commentCount").asInt()).isZero();
        assertThat(certifiedMeCard.get("reactionCount").asInt()).isZero();
        assertThat(certifiedMeCard.get("challengeRecordId").asLong()).isEqualTo(meChallengeRecordId);

        JsonNode afterRecordCommentsBeforeCreate = getJson(
                "/challenge-records/" + meChallengeRecordId + "/comments",
                meBearer
        );
        assertThat(afterRecordCommentsBeforeCreate.get("totalCount").asInt()).isZero();

        JsonNode afterRecordComment = postJson(
                "/challenge-records/" + meChallengeRecordId + "/comments",
                memberBearer,
                """
                        { "commentBody": "인증 완료 축하해요" }
                        """
        );
        assertThat(afterRecordComment.get("challengeRecordId").asLong()).isEqualTo(meChallengeRecordId);
        assertThat(afterRecordComment.get("commentBody").asText()).isEqualTo("인증 완료 축하해요");

        JsonNode createdReaction = postJson(
                "/challenge-records/" + meChallengeRecordId + "/reactions",
                memberBearer,
                """
                        { "reactionCode": "CLAP" }
                        """
        );
        assertThat(createdReaction.get("challengeRecordId").asLong()).isEqualTo(meChallengeRecordId);
        assertThat(createdReaction.get("reactionBody").asText()).isEqualTo("CLAP");

        JsonNode afterDetail = getJson(
                "/group-challenges/" + groupChallengeId + "/challenge-records/" + meChallengeRecordId,
                meBearer
        );
        assertThat(afterDetail.get("dailyStatus").asText()).isEqualTo("GOAL_ACHIEVED");
        assertThat(afterDetail.get("commentCount").asInt()).isEqualTo(1);
        assertThat(afterDetail.get("reactionCount").asInt()).isEqualTo(1);
        assertThat(afterDetail.at("/reactions/totalCount").asInt()).isEqualTo(1);
        assertThat(afterDetail.at("/reactions/summary/0/reactionBody").asText()).isEqualTo("CLAP");
        assertThat(afterDetail.at("/activityRecord/details/0/usedMinutes").asInt()).isEqualTo(70);
        assertThat(afterDetail.at("/activityRecord/details/0/isAchieved").asBoolean()).isTrue();

        clock.moveTo(TODAY.plusDays(1));

        JsonNode nextDayCalendar = getJson(
                "/group-challenges/" + groupChallengeId + "/activity-calendar",
                meBearer
        );
        assertThat(nextDayCalendar.at("/summary/startDate").asText()).isEqualTo("2026-04-08");
        assertThat(nextDayCalendar.at("/summary/endDate").asText()).isEqualTo("2026-04-16");
        assertThat(nextDayCalendar.at("/summary/allCount").asInt()).isEqualTo(5);
        assertThat(nextDayCalendar.at("/summary/halfCount").asInt()).isEqualTo(4);
        assertThat(nextDayCalendar.at("/summary/resetCount").asInt()).isZero();
        assertThat(nextDayCalendar.get("streakDays").asInt()).isEqualTo(9);

        JsonNode confirmedTodayHistory = getJson(
                "/group-challenges/" + groupChallengeId + "/challenge-records?date=2026-04-16",
                meBearer
        );
        assertThat(confirmedTodayHistory.at("/dailySummary/dayStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmedTodayHistory.at("/dailySummary/result").asText()).isEqualTo("HALF");
        assertThat(confirmedTodayHistory.at("/dailySummary/activeMemberCount").asInt()).isEqualTo(3);
        assertThat(confirmedTodayHistory.at("/dailySummary/certifiedMemberCount").asInt()).isEqualTo(2);
        assertThat(confirmedTodayHistory.at("/dailySummary/requiredCount").asInt()).isEqualTo(2);
    }

    private JsonNode getJson(String path, String bearer) throws Exception {
        HttpResponse<String> response = send("GET", path, bearer, null);
        assertThat(response.statusCode()).as("GET " + path + " -> " + response.body()).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    private JsonNode postJson(String path, String bearer, String body) throws Exception {
        HttpResponse<String> response = send("POST", path, bearer, body);
        assertThat(response.statusCode()).as("POST " + path + " -> " + response.body()).isIn(200, 201);
        return objectMapper.readTree(response.body());
    }

    private JsonNode postJson(String path, String bearer) throws Exception {
        HttpResponse<String> response = send("POST", path, bearer, null);
        assertThat(response.statusCode()).as("POST " + path + " -> " + response.body()).isIn(200, 201);
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

    private JsonNode meMember(JsonNode feed) {
        for (JsonNode member : feed.get("members")) {
            if (member.get("isMe").asBoolean()) {
                return member;
            }
        }
        throw new AssertionError("isMe member card not found: " + feed);
    }

    private JsonNode memberByDisplayName(JsonNode feed, String displayName) {
        for (JsonNode member : feed.get("members")) {
            if (displayName.equals(member.get("displayName").asText())) {
                return member;
            }
        }
        throw new AssertionError("member card not found: " + displayName + " in " + feed);
    }

    static class MutableClock extends Clock {

        private volatile Instant instant;

        MutableClock(LocalDate date) {
            this.instant = date.atTime(9, 0).atZone(KST).toInstant();
        }

        void moveTo(LocalDate date) {
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
}
