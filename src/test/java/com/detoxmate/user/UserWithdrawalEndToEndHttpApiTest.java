package com.detoxmate.user;

import com.detoxmate.user.controller.DevAuthController;
import com.detoxmate.user.service.AuthService;
import com.detoxmate.user.service.DevAuthService;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(UserWithdrawalEndToEndHttpApiTest.DevAuthTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserWithdrawalEndToEndHttpApiTest {

    @DynamicPropertySource
    static void useIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:user-withdrawal-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("test auth API로 만든 사용자가 회원 탈퇴하면 그룹 이력과 인증 정보는 익명화된 탈퇴 상태로 남는다")
    void withdrawUser_keepsGroupHistoryAnonymizedThroughHttpApis() throws Exception {
        JsonNode ownerLogin = postJson(
                "/dev/auth/test-login",
                null,
                """
                        { "testUserKey": "front-a" }
                        """,
                200
        );
        long ownerId = ownerLogin.get("id").asLong();
        String ownerBearer = bearer(ownerLogin.get("accessToken").asText());
        assertThat(ownerLogin.get("refreshToken").asText()).isNotBlank();
        assertThat(ownerLogin.get("isNewUser").asBoolean()).isTrue();

        JsonNode createdGroup = postJson(
                "/groups",
                ownerBearer,
                """
                        { "name": "탈퇴검증" }
                        """,
                201
        );
        long groupId = createdGroup.get("id").asLong();
        String inviteCode = createdGroup.get("inviteCode").asText();
        assertThat(inviteCode).isNotBlank();
        assertThat(createdGroup.get("myRole").asText()).isEqualTo("OWNER");
        assertThat(createdGroup.get("currentChallenge").get("id").asLong()).isPositive();
        assertThat(createdGroup.get("currentChallenge").get("status").asText()).isEqualTo("RECRUITING");

        JsonNode memberLogin = postJson(
                "/dev/auth/test-login",
                null,
                """
                        { "testUserKey": "front-b" }
                        """,
                200
        );
        long memberId = memberLogin.get("id").asLong();
        String memberBearer = bearer(memberLogin.get("accessToken").asText());
        assertThat(memberLogin.get("refreshToken").asText()).isNotBlank();
        assertThat(memberLogin.get("isNewUser").asBoolean()).isTrue();

        JsonNode joinedGroup = postJson(
                "/groups/join",
                memberBearer,
                """
                        { "inviteCode": "%s" }
                        """.formatted(inviteCode),
                200
        );
        assertThat(joinedGroup.get("members")).hasSize(2);
        JsonNode joinedMember = memberByUserId(joinedGroup, memberId);
        assertThat(joinedMember.get("role").asText()).isEqualTo("MEMBER");
        assertThat(joinedMember.get("status").asText()).isEqualTo("ACTIVE");

        HttpResponse<String> withdrawResponse = send("DELETE", "/users/me", ownerBearer, null);
        assertThat(withdrawResponse.statusCode()).as(withdrawResponse.body()).isEqualTo(204);

        HttpResponse<String> withdrawnUserProfileResponse = send("GET", "/users/me", ownerBearer, null);
        assertThat(withdrawnUserProfileResponse.statusCode())
                .as(withdrawnUserProfileResponse.body())
                .isEqualTo(401);

        JsonNode groupAfterWithdrawal = getJson("/groups/" + groupId, memberBearer);
        assertThat(groupAfterWithdrawal.get("myRole").asText()).isEqualTo("OWNER");
        assertThat(groupAfterWithdrawal.get("members")).hasSize(1);
        assertThat(hasMemberByUserId(groupAfterWithdrawal, ownerId)).isFalse();

        JsonNode remainingMember = memberByUserId(groupAfterWithdrawal, memberId);
        assertThat(remainingMember.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(remainingMember.get("role").asText()).isEqualTo("OWNER");
        assertThat(remainingMember.get("isUserWithdrawn").asBoolean()).isFalse();

        assertDatabaseState(ownerId, memberId, groupId);
    }

    private void assertDatabaseState(long ownerId, long memberId, long groupId) {
        assertThat(queryString("SELECT status FROM users WHERE user_id = ?", ownerId)).isEqualTo("WITHDRAWN");
        assertThat(queryTimestamp("SELECT withdrawn_at FROM users WHERE user_id = ?", ownerId)).isNotNull();
        assertThat(queryString("SELECT display_name FROM users WHERE user_id = ?", ownerId)).isEqualTo("탈퇴한 사용자");
        assertThat(queryString("SELECT profile_image_object_key FROM users WHERE user_id = ?", ownerId)).isNull();

        assertThat(queryString("SELECT status FROM group_members WHERE group_id = ? AND user_id = ?", groupId, ownerId))
                .isEqualTo("LEFT");
        assertThat(queryTimestamp("SELECT left_at FROM group_members WHERE group_id = ? AND user_id = ?", groupId, ownerId))
                .isNotNull();

        assertThat(queryString(
                """
                        SELECT gcp.status
                        FROM group_challenge_participants gcp
                        JOIN group_members gm ON gm.group_member_id = gcp.group_member_id
                        JOIN group_challenges gc ON gc.group_challenge_id = gcp.group_challenge_id
                        WHERE gm.group_id = ? AND gm.user_id = ?
                        ORDER BY gc.challenge_no DESC, gcp.group_challenge_participant_id DESC
                        LIMIT 1
                        """,
                groupId,
                ownerId
        )).isEqualTo("WITHDRAWN");
        assertThat(queryTimestamp(
                """
                        SELECT gcp.withdrawn_at
                        FROM group_challenge_participants gcp
                        JOIN group_members gm ON gm.group_member_id = gcp.group_member_id
                        JOIN group_challenges gc ON gc.group_challenge_id = gcp.group_challenge_id
                        WHERE gm.group_id = ? AND gm.user_id = ?
                        ORDER BY gc.challenge_no DESC, gcp.group_challenge_participant_id DESC
                        LIMIT 1
                        """,
                groupId,
                ownerId
        )).isNotNull();

        assertThat(queryString("SELECT status FROM group_members WHERE group_id = ? AND user_id = ?", groupId, memberId))
                .isEqualTo("ACTIVE");
        assertThat(queryString("SELECT role FROM group_members WHERE group_id = ? AND user_id = ?", groupId, memberId))
                .isEqualTo("OWNER");
        assertThat(queryInteger("SELECT COUNT(*) FROM social_login_users WHERE user_id = ?", ownerId)).isZero();
        assertThat(queryInteger("SELECT COUNT(*) FROM refresh_token_session WHERE user_id = ?", ownerId)).isZero();
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

    private JsonNode memberByUserId(JsonNode group, long userId) {
        for (JsonNode member : group.get("members")) {
            if (member.get("userId").asLong() == userId) {
                return member;
            }
        }
        throw new AssertionError("member userId=" + userId + " not found: " + group);
    }

    private boolean hasMemberByUserId(JsonNode group, long userId) {
        for (JsonNode member : group.get("members")) {
            if (member.get("userId").asLong() == userId) {
                return true;
            }
        }
        return false;
    }

    private String queryString(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    private Integer queryInteger(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private Timestamp queryTimestamp(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Timestamp.class, args);
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
