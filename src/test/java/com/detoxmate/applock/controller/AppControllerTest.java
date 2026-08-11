package com.detoxmate.applock.controller;

import com.detoxmate.applock.repository.AppRepository;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class AppControllerTest {

    private static final String APPS_URL = "/me/apps";
    private static final String APP_URL = "/me/apps/{appId}";
    private static final Long MISSING_APP_ID = Long.MAX_VALUE;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AppRepository appRepository;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long currentUserId;

    @BeforeEach
    void setUp() {
        currentUserId = userRepository.save(User.createNew("xeulbn")).getId();
    }

    @Test
    @DisplayName("POST /me/apps — 인증 사용자가 앱 잠금을 생성하면 201과 생성 결과를 반환한다")
    void create_returnsCreatedApp_whenAuthenticatedRequestIsValid() throws Exception {
        // given
        ObjectNode requestBody = appRequest(currentUserId, "Instagram", 60);

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appId").exists())
                .andExpect(jsonPath("$.userId").value(currentUserId))
                .andExpect(jsonPath("$.appDisplayName").value("Instagram"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(60));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointCases")
    @DisplayName("App lock API — Authorization이 없으면 401을 반환한다")
    void endpoints_returnUnauthorized_whenAuthorizationIsMissing(EndpointCase endpointCase) throws Exception {
        // given
        Long appId = createFixtureFor(endpointCase);

        // when & then
        performEndpoint(endpointCase, appId, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
        assertMutationStatePreserved(endpointCase, appId);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointCases")
    @DisplayName("App lock API — Authorization token이 malformed이면 401을 반환한다")
    void endpoints_returnUnauthorized_whenAuthorizationIsMalformed(EndpointCase endpointCase) throws Exception {
        // given
        Long appId = createFixtureFor(endpointCase);

        // when & then
        performEndpoint(endpointCase, appId, "Bearer malformed-token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
        assertMutationStatePreserved(endpointCase, appId);
    }

    @Test
    @DisplayName("GET /me/apps — 서명은 유효하지만 사용자가 존재하지 않는 token이면 401을 반환한다")
    void list_returnsUnauthorized_whenSignedTokenUserDoesNotExist() throws Exception {
        mockMvc.perform(get(APPS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mutatingEndpointCases")
    @DisplayName("POST/PUT/DELETE App lock API — 서명은 유효하지만 사용자가 없는 token이면 401이고 기존 상태를 보존한다")
    void mutatingEndpoints_returnUnauthorizedAndPreserveOwnerState_whenSignedTokenUserDoesNotExist(
            EndpointCase endpointCase
    ) throws Exception {
        // given
        Long appId = createAppThroughApi(currentUserId, "Instagram", 60);
        String nonexistentUserBearer = bearer(Long.MAX_VALUE);

        // when & then
        performEndpoint(endpointCase, appId, nonexistentUserBearer)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        getApp(currentUserId, appId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.appDisplayName").value("Instagram"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(60));

        assertThat(appRepository.findAllByUser_Id(currentUserId))
                .singleElement()
                .satisfies(persisted -> {
                    assertThat(persisted.getId()).isEqualTo(appId);
                    assertThat(persisted.getAppDisplayName()).isEqualTo("Instagram");
                    assertThat(persisted.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(60);
                });
    }

    @Test
    @DisplayName("POST /me/apps — 요청 사용자와 인증 사용자가 다르면 403을 반환한다")
    void create_returnsForbidden_whenRequestUserDoesNotMatchCurrentUser() throws Exception {
        // given
        Long otherUserId = saveUser("other");
        ObjectNode requestBody = appRequest(otherUserId, "Instagram", 60);

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isForbidden());
        getApps(currentUserId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apps.length()").value(0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCreateCases")
    @DisplayName("POST /me/apps — 필수 생성 필드 shape가 유효하지 않으면 400이고 App을 저장하지 않는다")
    void create_returnsBadRequestAndPersistsNothing_whenRequiredShapeIsInvalid(
            InvalidCreateCase invalidCreateCase
    ) throws Exception {
        // given
        ObjectNode requestBody = appRequest(currentUserId, "Instagram", 60);
        invalidCreateCase.makeInvalid(requestBody);

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));
        assertThat(appRepository.findAllByUser_Id(currentUserId)).isEmpty();
    }

    @Test
    @DisplayName("POST /me/apps — userId가 null이면 400을 반환한다")
    void create_returnsBadRequest_whenUserIdIsNull() throws Exception {
        // given
        ObjectNode requestBody = appRequest(null, "Instagram", 60);

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isBadRequest());
        assertThat(appRepository.findAllByUser_Id(currentUserId)).isEmpty();
    }

    @Test
    @DisplayName("POST /me/apps — userId가 누락되면 400을 반환한다")
    void create_returnsBadRequest_whenUserIdIsMissing() throws Exception {
        // given
        ObjectNode requestBody = appRequest(currentUserId, "Instagram", 60);
        requestBody.remove("userId");

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isBadRequest());
        assertThat(appRepository.findAllByUser_Id(currentUserId)).isEmpty();
    }

    @Test
    @DisplayName("POST /me/apps — 앱 표시 이름이 blank이면 400을 반환한다")
    void create_returnsBadRequest_whenAppDisplayNameIsBlank() throws Exception {
        // given
        ObjectNode requestBody = appRequest(currentUserId, "   ", 60);

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isBadRequest());
        assertThat(appRepository.findAllByUser_Id(currentUserId)).isEmpty();
    }

    @Test
    @DisplayName("POST /me/apps — 앱 표시 이름이 100자를 초과하면 400을 반환한다")
    void create_returnsBadRequest_whenAppDisplayNameExceeds100Characters() throws Exception {
        // given
        ObjectNode requestBody = appRequest(currentUserId, "a".repeat(101), 60);

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isBadRequest());
        assertThat(appRepository.findAllByUser_Id(currentUserId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1441})
    @DisplayName("POST /me/apps — 일일 제한 시간이 허용 범위를 벗어나면 400을 반환한다")
    void create_returnsBadRequest_whenDailyLimitMinutesIsOutOfRange(int dailyLimitMinutes) throws Exception {
        // given
        ObjectNode requestBody = appRequest(currentUserId, "Instagram", dailyLimitMinutes);

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isBadRequest());
        assertThat(appRepository.findAllByUser_Id(currentUserId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1440})
    @DisplayName("POST /me/apps — 일일 제한 시간 경계값 0과 1440을 허용한다")
    void create_returnsCreatedApp_whenDailyLimitMinutesIsBoundary(int dailyLimitMinutes) throws Exception {
        // given
        ObjectNode requestBody = appRequest(currentUserId, "Boundary App", dailyLimitMinutes);

        // when & then
        postApp(currentUserId, requestBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(currentUserId))
                .andExpect(jsonPath("$.appDisplayName").value("Boundary App"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(dailyLimitMinutes));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    @DisplayName("POST와 PUT /me/apps — 앱 표시 이름 길이 경계값 1과 100을 허용한다")
    void createAndUpdate_acceptAppDisplayNameLengthBoundaries(int nameLength) throws Exception {
        // given
        String createName = "a".repeat(nameLength);
        String updateName = "b".repeat(nameLength);

        // when & then
        Long appId = createAppThroughApi(currentUserId, createName, 60);
        putApp(currentUserId, appId, appRequest(currentUserId, updateName, 60))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.appDisplayName").value(updateName));
        getApp(currentUserId, appId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appDisplayName").value(updateName));
    }

    @Test
    @DisplayName("GET /me/apps — 인증 사용자가 소유한 앱만 반환한다")
    void list_returnsOnlyCurrentUsersApps() throws Exception {
        // given
        Long currentUsersAppId = createAppThroughApi(currentUserId, "Instagram", 60);
        Long otherUserId = saveUser("other");
        createAppThroughApi(otherUserId, "Other App", 120);

        // when & then
        getApps(currentUserId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apps.length()").value(1))
                .andExpect(jsonPath("$.apps[0].appId").value(currentUsersAppId))
                .andExpect(jsonPath("$.apps[0].userId").value(currentUserId))
                .andExpect(jsonPath("$.apps[0].appDisplayName").value("Instagram"))
                .andExpect(jsonPath("$.apps[0].dailyLimitMinutes").value(60));
    }

    @Test
    @DisplayName("GET /me/apps — 소유한 앱이 없으면 빈 목록을 반환한다")
    void list_returnsEmptyList_whenCurrentUserHasNoApps() throws Exception {
        // when & then
        getApps(currentUserId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apps.length()").value(0));
    }

    @Test
    @DisplayName("GET /me/apps/{appId} — 소유한 앱을 조회하면 앱 정보를 반환한다")
    void get_returnsOwnedApp() throws Exception {
        // given
        Long appId = createAppThroughApi(currentUserId, "Instagram", 60);

        // when & then
        getApp(currentUserId, appId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.userId").value(currentUserId))
                .andExpect(jsonPath("$.appDisplayName").value("Instagram"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(60));
    }

    @Test
    @DisplayName("PUT /me/apps/{appId} — 소유한 앱의 이름과 일일 제한 시간을 변경한다")
    void update_changesOwnedAppsNameAndDailyLimit() throws Exception {
        // given
        Long appId = createAppThroughApi(currentUserId, "Instagram", 60);
        ObjectNode requestBody = appRequest(currentUserId, "YouTube", 120);

        // when & then
        putApp(currentUserId, appId, requestBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.userId").value(currentUserId))
                .andExpect(jsonPath("$.appDisplayName").value("YouTube"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(120));

        getApp(currentUserId, appId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.appDisplayName").value("YouTube"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(120));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUpdateCases")
    @DisplayName("PUT /me/apps/{appId} — 유효하지 않은 요청이면 400을 반환하고 기존 aggregate를 보존한다")
    void update_returnsBadRequestAndPreservesAggregate_whenRequestIsInvalid(
            InvalidUpdateCase invalidUpdateCase
    ) throws Exception {
        // given
        Long appId = createAppThroughApi(currentUserId, "Instagram", 60);
        ObjectNode requestBody = appRequest(currentUserId, "YouTube", 120);
        invalidUpdateCase.makeInvalid(requestBody);

        // when & then
        putApp(currentUserId, appId, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));
        getApp(currentUserId, appId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.appDisplayName").value("Instagram"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(60));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1440})
    @DisplayName("PUT /me/apps/{appId} — 일일 제한 시간 경계값 0과 1440을 허용한다")
    void update_acceptsDailyLimitMinuteBoundaries(int dailyLimitMinutes) throws Exception {
        // given
        Long appId = createAppThroughApi(currentUserId, "Instagram", 60);

        // when & then
        putApp(currentUserId, appId, appRequest(currentUserId, "Instagram", dailyLimitMinutes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(dailyLimitMinutes));
        getApp(currentUserId, appId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimitMinutes").value(dailyLimitMinutes));
    }

    @Test
    @DisplayName("PUT /me/apps/{appId} — 요청 사용자와 인증 사용자가 다르면 403을 반환하고 앱을 변경하지 않는다")
    void update_returnsForbidden_whenRequestUserDoesNotMatchCurrentUser() throws Exception {
        // given
        Long appId = createAppThroughApi(currentUserId, "Instagram", 60);
        Long otherUserId = saveUser("other");
        ObjectNode requestBody = appRequest(otherUserId, "YouTube", 120);

        // when & then
        putApp(currentUserId, appId, requestBody)
                .andExpect(status().isForbidden());
        getApp(currentUserId, appId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appDisplayName").value("Instagram"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(60));
    }

    @Test
    @DisplayName("DELETE /me/apps/{appId} — 소유한 앱을 삭제하면 204를 반환하고 더 이상 조회할 수 없다")
    void delete_removesOwnedApp() throws Exception {
        // given
        Long appId = createAppThroughApi(currentUserId, "Instagram", 60);

        // when & then
        deleteApp(currentUserId, appId)
                .andExpect(status().isNoContent());
        getApp(currentUserId, appId)
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("protectedOperations")
    @DisplayName("GET/PUT/DELETE /me/apps/{appId} — missing과 foreign 앱은 같은 404를 반환하고 소유자 상태를 보존한다")
    void protectedOperations_hideForeignAppLikeMissingAndPreserveOwnerState(
            ProtectedOperation operation
    ) throws Exception {
        // given
        Long otherUserId = saveUser("other");
        Long otherUsersAppId = createAppThroughApi(otherUserId, "Other App", 120);

        // when
        MvcResult missingResult = performProtectedOperation(operation, currentUserId, MISSING_APP_ID)
                .andExpect(status().isNotFound())
                .andReturn();
        MvcResult foreignResult = performProtectedOperation(operation, currentUserId, otherUsersAppId)
                .andExpect(status().isNotFound())
                .andReturn();

        // then
        ErrorBody missingError = errorBody(missingResult);
        ErrorBody foreignError = errorBody(foreignResult);
        assertThat(missingError.status()).isEqualTo(404);
        assertThat(foreignError).isEqualTo(missingError);

        getApp(otherUserId, otherUsersAppId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(otherUsersAppId))
                .andExpect(jsonPath("$.appDisplayName").value("Other App"))
                .andExpect(jsonPath("$.dailyLimitMinutes").value(120));
    }

    private static Stream<EndpointCase> endpointCases() {
        return Stream.of(EndpointCase.values());
    }

    private static Stream<EndpointCase> mutatingEndpointCases() {
        return Stream.of(EndpointCase.CREATE, EndpointCase.UPDATE, EndpointCase.DELETE);
    }

    private static Stream<InvalidCreateCase> invalidCreateCases() {
        return Stream.of(InvalidCreateCase.values());
    }

    private static Stream<InvalidUpdateCase> invalidUpdateCases() {
        return Stream.of(InvalidUpdateCase.values());
    }

    private static Stream<ProtectedOperation> protectedOperations() {
        return Stream.of(ProtectedOperation.values());
    }

    private Long createFixtureFor(EndpointCase endpointCase) throws Exception {
        if (!endpointCase.requiresExistingApp()) {
            return null;
        }
        return createAppThroughApi(currentUserId, "Instagram", 60);
    }

    private ResultActions performEndpoint(
            EndpointCase endpointCase,
            Long appId,
            String authorization
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = switch (endpointCase) {
            case CREATE -> post(APPS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(appRequest(currentUserId, "Instagram", 60)));
            case LIST -> get(APPS_URL);
            case GET -> get(APP_URL, appId);
            case UPDATE -> put(APP_URL, appId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(appRequest(currentUserId, "Changed", 120)));
            case DELETE -> delete(APP_URL, appId);
        };

        if (authorization != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mockMvc.perform(requestBuilder);
    }

    private void assertMutationStatePreserved(EndpointCase endpointCase, Long appId) throws Exception {
        if (endpointCase == EndpointCase.CREATE) {
            getApps(currentUserId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.apps.length()").value(0));
            return;
        }

        if (endpointCase == EndpointCase.UPDATE || endpointCase == EndpointCase.DELETE) {
            getApp(currentUserId, appId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appId").value(appId))
                    .andExpect(jsonPath("$.appDisplayName").value("Instagram"))
                    .andExpect(jsonPath("$.dailyLimitMinutes").value(60));
        }
    }

    private ResultActions performProtectedOperation(
            ProtectedOperation operation,
            Long authenticatedUserId,
            Long appId
    ) throws Exception {
        return switch (operation) {
            case GET -> getApp(authenticatedUserId, appId);
            case UPDATE -> putApp(
                    authenticatedUserId,
                    appId,
                    appRequest(authenticatedUserId, "Changed", 300)
            );
            case DELETE -> deleteApp(authenticatedUserId, appId);
        };
    }

    private ErrorBody errorBody(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new ErrorBody(
                body.path("code").asText(),
                body.path("message").asText(),
                body.path("status").asInt()
        );
    }

    private Long saveUser(String displayName) {
        return userRepository.save(User.createNew(displayName)).getId();
    }

    private Long createAppThroughApi(Long userId, String appDisplayName, int dailyLimitMinutes) throws Exception {
        String responseBody = postApp(userId, appRequest(userId, appDisplayName, dailyLimitMinutes))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).path("appId").asLong();
    }

    private ObjectNode appRequest(Long userId, String appDisplayName, Integer dailyLimitMinutes) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        if (userId == null) {
            requestBody.putNull("userId");
        } else {
            requestBody.put("userId", userId);
        }
        if (appDisplayName == null) {
            requestBody.putNull("appDisplayName");
        } else {
            requestBody.put("appDisplayName", appDisplayName);
        }
        if (dailyLimitMinutes == null) {
            requestBody.putNull("dailyLimitMinutes");
        } else {
            requestBody.put("dailyLimitMinutes", dailyLimitMinutes);
        }
        return requestBody;
    }

    private ResultActions postApp(Long authenticatedUserId, ObjectNode requestBody) throws Exception {
        return mockMvc.perform(post(APPS_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(authenticatedUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));
    }

    private ResultActions getApps(Long authenticatedUserId) throws Exception {
        return mockMvc.perform(get(APPS_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(authenticatedUserId)));
    }

    private ResultActions getApp(Long authenticatedUserId, Long appId) throws Exception {
        return mockMvc.perform(get(APP_URL, appId)
                .header(HttpHeaders.AUTHORIZATION, bearer(authenticatedUserId)));
    }

    private ResultActions putApp(Long authenticatedUserId, Long appId, ObjectNode requestBody) throws Exception {
        return mockMvc.perform(put(APP_URL, appId)
                .header(HttpHeaders.AUTHORIZATION, bearer(authenticatedUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));
    }

    private ResultActions deleteApp(Long authenticatedUserId, Long appId) throws Exception {
        return mockMvc.perform(delete(APP_URL, appId)
                .header(HttpHeaders.AUTHORIZATION, bearer(authenticatedUserId)));
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    private enum EndpointCase {
        CREATE("POST /me/apps", false),
        LIST("GET /me/apps", false),
        GET("GET /me/apps/{appId}", true),
        UPDATE("PUT /me/apps/{appId}", true),
        DELETE("DELETE /me/apps/{appId}", true);

        private final String label;
        private final boolean requiresExistingApp;

        EndpointCase(String label, boolean requiresExistingApp) {
            this.label = label;
            this.requiresExistingApp = requiresExistingApp;
        }

        boolean requiresExistingApp() {
            return requiresExistingApp;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum InvalidUpdateCase {
        MISSING_USER_ID("missing userId"),
        NULL_USER_ID("null userId"),
        MISSING_NAME("missing appDisplayName"),
        NULL_NAME("null appDisplayName"),
        EMPTY_NAME("empty appDisplayName"),
        BLANK_NAME("blank appDisplayName"),
        OVERLONG_NAME("101-character appDisplayName"),
        MISSING_LIMIT("missing dailyLimitMinutes"),
        NULL_LIMIT("null dailyLimitMinutes"),
        BELOW_MINIMUM_LIMIT("dailyLimitMinutes -1"),
        ABOVE_MAXIMUM_LIMIT("dailyLimitMinutes 1441");

        private final String label;

        InvalidUpdateCase(String label) {
            this.label = label;
        }

        void makeInvalid(ObjectNode requestBody) {
            switch (this) {
                case MISSING_USER_ID -> requestBody.remove("userId");
                case NULL_USER_ID -> requestBody.putNull("userId");
                case MISSING_NAME -> requestBody.remove("appDisplayName");
                case NULL_NAME -> requestBody.putNull("appDisplayName");
                case EMPTY_NAME -> requestBody.put("appDisplayName", "");
                case BLANK_NAME -> requestBody.put("appDisplayName", "   ");
                case OVERLONG_NAME -> requestBody.put("appDisplayName", "a".repeat(101));
                case MISSING_LIMIT -> requestBody.remove("dailyLimitMinutes");
                case NULL_LIMIT -> requestBody.putNull("dailyLimitMinutes");
                case BELOW_MINIMUM_LIMIT -> requestBody.put("dailyLimitMinutes", -1);
                case ABOVE_MAXIMUM_LIMIT -> requestBody.put("dailyLimitMinutes", 1441);
            }
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum InvalidCreateCase {
        MISSING_NAME("missing appDisplayName"),
        NULL_NAME("null appDisplayName"),
        EMPTY_NAME("empty appDisplayName"),
        MISSING_LIMIT("missing dailyLimitMinutes"),
        NULL_LIMIT("null dailyLimitMinutes");

        private final String label;

        InvalidCreateCase(String label) {
            this.label = label;
        }

        void makeInvalid(ObjectNode requestBody) {
            switch (this) {
                case MISSING_NAME -> requestBody.remove("appDisplayName");
                case NULL_NAME -> requestBody.putNull("appDisplayName");
                case EMPTY_NAME -> requestBody.put("appDisplayName", "");
                case MISSING_LIMIT -> requestBody.remove("dailyLimitMinutes");
                case NULL_LIMIT -> requestBody.putNull("dailyLimitMinutes");
            }
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum ProtectedOperation {
        GET,
        UPDATE,
        DELETE
    }

    private record ErrorBody(String code, String message, int status) {
    }
}
