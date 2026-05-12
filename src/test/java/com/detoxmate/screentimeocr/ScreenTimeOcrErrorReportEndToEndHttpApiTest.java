package com.detoxmate.screentimeocr;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UsageGoalTypeRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.domain.ChallengeRecordStatus;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ScreenTimeOcrErrorReportEndToEndHttpApiTest {

    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 5, 12);
    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger();

    @DynamicPropertySource
    static void useIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:screen-time-ocr-report-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
    }

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
    ActivityRecordRepository activityRecordRepository;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    ScreenTimeOcrErrorReportRepository reportRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("실제 HTTP API로 OCR 오류 신고 생성, admin 목록 조회, admin 보정까지 검증한다")
    void screenTimeOcrErrorReportFlow_correctsActualCertificationThroughHttpApis() throws Exception {
        Fixture fixture = saveFixture();
        String userBearer = bearer(fixture.user().getId());
        String adminBearer = bearer(fixture.admin().getId());

        JsonNode createdReport = postJson(
                "/screen-time-ocr-error-reports",
                userBearer,
                """
                        {
                          "activityRecordId": %d,
                          "groupChallengeParticipantId": %d,
                          "recordDate": "2026-05-12",
                          "imageObjectKey": "screen-time-ocr-reports/%d/2026/05/sample.png",
                          "ocrTotalUsedMinutes": 180
                        }
                        """.formatted(
                        fixture.activityRecord().getId(),
                        fixture.participant().getId(),
                        fixture.user().getId()
                ),
                201
        );

        long reportId = createdReport.get("id").asLong();
        assertThat(createdReport.get("status").asText()).isEqualTo("PENDING");

        JsonNode adminList = getJson(
                "/admin/screen-time-ocr-error-reports?status=PENDING&page=0&size=20",
                adminBearer
        );
        assertThat(adminList.get("totalElements").asLong()).isEqualTo(1L);
        assertThat(adminList.at("/items/0/id").asLong()).isEqualTo(reportId);
        assertThat(adminList.at("/items/0/ocrTotalUsedMinutes").asInt()).isEqualTo(180);
        assertThat(adminList.at("/items/0/imageUrl").asText())
                .isEqualTo("https://example.com/media/screen-time-ocr-reports/%d/2026/05/sample.png"
                        .formatted(fixture.user().getId()));

        JsonNode updatedReport = patchJson(
                "/admin/screen-time-ocr-error-reports/" + reportId,
                adminBearer,
                """
                        {
                          "action": "CORRECT",
                          "correctedTotalUsedMinutes": 100,
                          "adminNote": "실제 HTTP API 검수"
                        }
                        """,
                200
        );
        assertThat(updatedReport.get("status").asText()).isEqualTo("CORRECTED");
        assertThat(updatedReport.get("correctedTotalUsedMinutes").asInt()).isEqualTo(100);
        assertThat(updatedReport.get("resolvedByUserId").asLong()).isEqualTo(fixture.admin().getId());

        ActivityRecord correctedActivityRecord = activityRecordRepository
                .findByIdWithDetails(fixture.activityRecord().getId())
                .orElseThrow();
        ActivityRecordDetail totalUsageDetail = correctedActivityRecord.getDetails().getFirst();
        assertThat(totalUsageDetail.getUseMinutes()).isEqualTo(100);
        assertThat(totalUsageDetail.isAchieved()).isTrue();

        ChallengeRecord correctedChallengeRecord = challengeRecordRepository
                .findByActivityRecordId(fixture.activityRecord().getId())
                .orElseThrow();
        assertThat(correctedChallengeRecord.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_SUCCESS);

        ScreenTimeOcrErrorReport correctedReport = reportRepository.findById(reportId).orElseThrow();
        assertThat(correctedReport.getStatus()).isEqualTo(ScreenTimeOcrErrorReportStatus.CORRECTED);
        assertThat(correctedReport.getCorrectedTotalUsedMinutes()).isEqualTo(100);
        assertThat(correctedReport.getAdminNote()).isEqualTo("실제 HTTP API 검수");
    }

    @Test
    @DisplayName("실제 HTTP API에서 non-admin은 admin 목록 API를 호출할 수 없다")
    void adminList_rejectsNonAdminThroughHttpApi() throws Exception {
        Fixture fixture = saveFixture();

        HttpResponse<String> response = send(
                "GET",
                "/admin/screen-time-ocr-error-reports?status=PENDING",
                bearer(fixture.user().getId()),
                null
        );

        assertThat(response.statusCode()).as(response.body()).isEqualTo(403);
    }

    private Fixture saveFixture() {
        int sequence = FIXTURE_SEQUENCE.incrementAndGet();
        UsageGoalType totalUsage = usageGoalTypeRepository.save(
                UsageGoalType.create((long) sequence, UsageGoalTypeCode.TOTAL_USAGE)
        );
        User user = userRepository.save(User.createNew("신고자"));
        User admin = User.createNew("관리자");
        admin.grantAdminRole();
        userRepository.save(admin);

        Group group = groupRepository.save(Group.createNew("OCR검수" + sequence, "OCR0" + sequence));
        GroupChallenge challenge = GroupChallenge.createFirst(group.getId());
        challenge.activate(LocalDateTime.of(2026, 5, 10, 0, 0));
        groupChallengeRepository.save(challenge);

        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), group.getId()));
        GroupChallengeParticipant participant = participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), challenge.getId())
        );
        UserUsageGoalTime goal = saveGoal(user, totalUsage, 120, LocalDateTime.of(2026, 5, 11, 9, 0));

        ActivityRecord activityRecord = ActivityRecord.create(
                user,
                participant,
                "activity-records/e2e/screen-time.png",
                "OCR이 더 크게 읽은 신고 케이스"
        );
        activityRecord.addDetail(goal, 180, false);
        ActivityRecord savedActivityRecord = activityRecordRepository.saveAndFlush(activityRecord);

        ChallengeRecord challengeRecord = challengeRecordRepository.saveAndFlush(
                ChallengeRecord.create(challenge.getId(), participant.getId(), RECORD_DATE)
        );
        challengeRecord.certify(
                savedActivityRecord.getId(),
                participant.getId(),
                ChallengeRecordCertificationResult.FAIL
        );
        challengeRecordRepository.saveAndFlush(challengeRecord);

        return new Fixture(user, admin, participant, savedActivityRecord);
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
        return goal;
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

    private JsonNode patchJson(String path, String bearer, String body, int expectedStatus) throws Exception {
        HttpResponse<String> response = send("PATCH", path, bearer, body);
        assertThat(response.statusCode()).as("PATCH " + path + " -> " + response.body()).isEqualTo(expectedStatus);
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

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    private record Fixture(
            User user,
            User admin,
            GroupChallengeParticipant participant,
            ActivityRecord activityRecord
    ) {
    }
}
