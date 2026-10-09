package com.detoxmate.user;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(UserProfileEndToEndHttpApiTest.DevAuthTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserProfileEndToEndHttpApiTest {

    @DynamicPropertySource
    static void useIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:user-profile-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
    }

    @LocalServerPort
    int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("실제 v3 API 문서는 프로필과 친구 초대 응답의 필수 필드 및 null 허용을 일치시킨다")
    void apiDocs_exposesFriendInviteResponseContracts() throws Exception {
        // when
        HttpResponse<String> response = send("GET", "/v3/api-docs", null, null);

        // then
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        JsonNode schemas = objectMapper.readTree(response.body()).path("components").path("schemas");
        Map<String, Set<String>> contracts = Map.of(
                "MyPageResponse", Set.of("id", "displayName", "userCode", "profileImageUrl", "pushNotificationEnabled"),
                "FriendListUserResponse", Set.of("userId", "displayName", "userCode", "profileImageUrl", "relationshipStatus", "requestId"),
                "FriendInviteResponse", Set.of("code", "userCode"),
                "FriendResponse", Set.of("friendshipId", "user", "acceptedAt"),
                "FriendReceivedRequestResponse", Set.of("requestId", "user", "createdAt"));
        contracts.forEach((name, fields) -> {
            JsonNode schema = schemas.path(name);
            Set<String> properties = new HashSet<>();
            schema.path("properties").fieldNames().forEachRemaining(properties::add);
            assertThat(properties).as(name + " properties").isEqualTo(fields);
            Set<String> required = new HashSet<>();
            schema.path("required").forEach(field -> required.add(field.asText()));
            assertThat(required).as(name + " required").isEqualTo(fields);
            fields.forEach(field -> {
                JsonNode property = schema.path("properties").path(field);
                boolean nullable = property.path("nullable").asBoolean(false);
                for (JsonNode type : property.path("type")) {
                    nullable |= "null".equals(type.asText());
                }
                assertThat(nullable).as(name + "." + field + " nullable")
                        .isEqualTo("profileImageUrl".equals(field)
                                || ("FriendListUserResponse".equals(name) && "requestId".equals(field)));
            });
        });
    }

    @Test
    @DisplayName("프로필 수정 API는 공백 없이 이모지를 포함한 5자까지 저장하고 거절한 요청은 기존 이름을 유지한다")
    void updateMyProfile_enforcesFiveCodePointsWithoutWhitespaceThroughHttpApi() throws Exception {
        // given
        JsonNode login = postJson(
                "/dev/auth/test-login",
                null,
                """
                        { "testUserKey": "front-c" }
                        """,
                200
        );
        String bearer = bearer(login.get("accessToken").asText());

        // when & then
        JsonNode oneCharacterResponse = patchJson(
                "/users/me",
                bearer,
                """
                        { "displayName": "가" }
                        """,
                200
        );
        assertThat(oneCharacterResponse.get("displayName").asText()).isEqualTo("가");

        JsonNode fiveCharacterResponse = patchJson(
                "/users/me",
                bearer,
                """
                        { "displayName": "12345" }
                        """,
                200
        );
        assertThat(fiveCharacterResponse.get("displayName").asText()).isEqualTo("12345");

        JsonNode emojiResponse = patchJson(
                "/users/me",
                bearer,
                """
                        { "displayName": "😀😁😂😃😄" }
                        """,
                200
        );
        assertThat(emojiResponse.get("displayName").asText()).isEqualTo("😀😁😂😃😄");

        HttpResponse<String> invalidResponse = send(
                "PATCH",
                "/users/me",
                bearer,
                """
                        { "displayName": "123456" }
                        """
        );
        assertThat(invalidResponse.statusCode()).as(invalidResponse.body()).isEqualTo(400);

        HttpResponse<String> whitespaceResponse = send(
                "PATCH",
                "/users/me",
                bearer,
                """
                        { "displayName": "의 진" }
                        """
        );
        assertThat(whitespaceResponse.statusCode()).as(whitespaceResponse.body()).isEqualTo(400);

        JsonNode nullNameResponse = patchJson("/users/me", bearer, "{\"displayName\":null}", 200);
        assertThat(nullNameResponse.get("displayName").asText()).isEqualTo("😀😁😂😃😄");

        JsonNode omittedNameResponse = patchJson("/users/me", bearer, "{}", 200);
        assertThat(omittedNameResponse.get("displayName").asText()).isEqualTo("😀😁😂😃😄");

        HttpResponse<String> persistedProfile = send("GET", "/users/me", bearer, null);
        assertThat(persistedProfile.statusCode()).as(persistedProfile.body()).isEqualTo(200);
        assertThat(objectMapper.readTree(persistedProfile.body()).get("displayName").asText())
                .isEqualTo("😀😁😂😃😄");
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

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
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
