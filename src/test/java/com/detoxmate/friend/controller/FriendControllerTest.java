package com.detoxmate.friend.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.friend.dto.FriendInviteResponse;
import com.detoxmate.friend.dto.FriendInviteeResponse;
import com.detoxmate.friend.dto.FriendListUserResponse;
import com.detoxmate.friend.dto.FriendReceivedRequestResponse;
import com.detoxmate.friend.dto.FriendSearchResponse;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.dto.FriendRequestResponse;
import com.detoxmate.friend.dto.FriendResponse;
import com.detoxmate.friend.dto.FriendUserResponse;
import com.detoxmate.friend.service.FriendService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
import com.epages.restdocs.apispec.EnumFields;
import org.springframework.restdocs.constraints.Constraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class FriendControllerTest {

    private FriendService friendService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        friendService = mock(FriendService.class);
        UserService userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "나", "https://example.com/profile.png", true));

        mockMvc = MockMvcBuilders.standaloneSetup(new FriendController(friendService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(com.detoxmate.common.error.GlobalExceptionHandlerTestFixture.globalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    @DisplayName("내 초대코드를 조회하면 코드를 반환한다")
    void getMyInvite_returnsInviteCode() throws Exception {
        when(friendService.getMyInvite(1L)).thenReturn(new FriendInviteResponse("a".repeat(64), "me@example.com"));
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] responseFields = inviteResponseFields();

        mockMvc.perform(get("/friends/invite").header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("a".repeat(64)))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andDo(document("friends/invite-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Get my friend invite code")
                                .description("로그인 사용자의 고정 초대코드와 공유용 본인 이메일을 조회한다. 자동 만료·재발급은 없으며 반복 조회에도 같은 코드를 반환한다. 이메일이 없는 기존 계정은 email=null이며 링크로 공유한다.")
                                .requestHeaders(headers)
                                .responseSchema(schema("FriendInviteResponse"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("초대코드로 친구를 조회하면 친구 공개 정보와 관계 상태를 반환한다")
    void getInvitee_returnsInvitedUser() throws Exception {
        FriendInviteeResponse response = new FriendInviteeResponse(2L, "친구", null,
                FriendRelationshipStatus.NONE, null, 5L, 3L);
        when(friendService.getInvitee("invite-code", 1L)).thenReturn(response);
        HeaderDescriptor[] headers = authorizationHeaders();
        ParameterDescriptor[] pathParameters = codePathParameters();
        FieldDescriptor[] responseFields = inviteeResponseFields();

        mockMvc.perform(get("/friends/invite/{code}", "invite-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.relationshipStatus").value("NONE"))
                .andExpect(jsonPath("$.daysSinceStart").value(5))
                .andExpect(jsonPath("$.targetSuccessCount").value(3))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andDo(document("friends/invitee-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        pathParameters(pathParameters),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Get user by friend invite code")
                                .description("활성 초대 소유자의 이름·사진·관계 상태와 공개 요약 통계를 조회한다. 가입 당일을 1일로 세며 Asia/Seoul 날짜 기준이다. 성공 횟수는 모든 그룹의 성공한 챌린지 기록 수이며 성공 날짜 수가 아니다. 조회만으로 요청·친구 관계가 생기지 않는다.")
                                .requestHeaders(headers)
                                .pathParameters(codePathParametersForOpenApi())
                                .responseSchema(schema("FriendInviteeResponse"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("이메일로 친구를 검색하면 정확히 일치하는 사용자를 반환한다")
    void searchByEmail_returnsExactMatch() throws Exception {
        when(friendService.searchByEmail("friend@example.com", 1L))
                .thenReturn(new FriendSearchResponse(2L, "친구", null, FriendRelationshipStatus.NONE,
                        null, 2L, "공통 친구"));
        HeaderDescriptor[] headers = authorizationHeaders();
        ParameterDescriptor[] queryParameters = emailQueryParameters();
        FieldDescriptor[] responseFields = searchResponseFields();

        mockMvc.perform(get("/friends/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .param("email", "friend@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.relationshipStatus").value("NONE"))
                .andExpect(jsonPath("$.mutualFriendCount").value(2))
                .andExpect(jsonPath("$.mutualFriendPreviewName").value("공통 친구"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andDo(document("friends/search-email-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        queryParameters(queryParameters),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Search friend by exact email")
                                .description("정규화한 이메일 전체 주소와 정확히 일치하는 활성 사용자를 조회한다. 현재 양쪽의 수락된 관계 중 활성 공통 친구 수와 대표 이름을 반환한다. 대표는 가장 작은 사용자 ID이며 공통 친구가 없거나 자기 자신이면 0/null이다. 대기 요청은 집계하지 않는다. 이메일·상세 활동은 응답에 포함하지 않는다.")
                                .requestHeaders(headers)
                                .queryParameters(emailQueryParametersForOpenApi())
                                .responseSchema(schema("FriendSearchResponse"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("친구 요청 대상이 없으면 400 에러를 반환한다")
    void sendRequest_returnsBadRequestWhenTargetIsMissing() throws Exception {
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] errorFields = errorResponseFields();

        mockMvc.perform(post("/friends/requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("friends/requests-create-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        responseFields(errorFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Send friend request")
                                .description("로그인 사용자가 다른 활성 사용자에게 친구 요청을 보낸다.")
                                .requestHeaders(headers)
                                .requestSchema(schema("CreateFriendRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .responseFields(errorFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("친구 요청을 보내면 201과 요청 정보를 반환한다")
    void sendRequest_returnsCreatedRequest() throws Exception {
        FriendRequestResponse response = requestResponse(101L, 2L, "친구", FriendRelationshipStatus.PENDING_SENT, 101L);
        when(friendService.sendRequest(1L, 2L)).thenReturn(response);
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] requestFields = createRequestFields();
        FieldDescriptor[] responseFields = requestResponseFields();

        mockMvc.perform(post("/friends/requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "targetUserId": 2 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(101))
                .andExpect(jsonPath("$.user.userId").value(2))
                .andExpect(jsonPath("$.user.relationshipStatus").value("PENDING_SENT"))
                .andDo(document("friends/requests-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        requestFields(requestFields),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Send friend request")
                                .description("로그인 사용자가 다른 활성 사용자에게 친구 요청을 보낸다.")
                                .requestHeaders(headers)
                                .requestSchema(schema("CreateFriendRequest"))
                                .responseSchema(schema("FriendRequestResponse"))
                                .requestFields(requestFields)
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("보낸 친구 요청 목록을 조회하면 대기 중인 요청 목록을 반환한다")
    void getSentRequests_returnsPendingRequestsSentByMe() throws Exception {
        when(friendService.getSentRequests(1L)).thenReturn(List.of(
                requestResponse(101L, 2L, "친구", FriendRelationshipStatus.PENDING_SENT, 101L)
        ));
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] responseFields = requestResponseListFields();

        mockMvc.perform(get("/friends/requests/sent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(101))
                .andExpect(jsonPath("$[0].user.relationshipStatus").value("PENDING_SENT"))
                .andDo(document("friends/requests-sent-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("List sent friend requests")
                                .description("로그인 사용자가 보낸 대기 중인 친구 요청 목록을 조회한다.")
                                .requestHeaders(headers)
                                .responseSchema(schema("FriendRequestResponseList"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("받은 친구 요청 목록을 조회하면 대기 중인 요청 목록을 반환한다")
    void getReceivedRequests_returnsPendingRequestsReceivedByMe() throws Exception {
        when(friendService.getReceivedRequests(1L)).thenReturn(List.of(
                new FriendReceivedRequestResponse(101L,
                        listUserResponse(2L, "친구", FriendRelationshipStatus.PENDING_RECEIVED, 101L),
                        LocalDateTime.of(2026, 9, 14, 17, 0))
        ));
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] responseFields = receivedRequestResponseListFields();

        mockMvc.perform(get("/friends/requests/received")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(101))
                .andExpect(jsonPath("$[0].user.relationshipStatus").value("PENDING_RECEIVED"))
                .andExpect(jsonPath("$[0].user.email").value("friend@example.com"))
                .andDo(document("friends/requests-received-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("List received friend requests")
                                .description("로그인 사용자가 받은 대기 중인 친구 요청 목록을 조회한다.")
                                .requestHeaders(headers)
                                .responseSchema(schema("FriendReceivedRequestResponseList"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("받은 친구 요청을 수락하면 친구 관계 정보를 반환한다")
    void acceptRequest_returnsAcceptedFriendship() throws Exception {
        when(friendService.acceptRequest(101L, 1L)).thenReturn(
                new FriendResponse(
                        101L,
                        listUserResponse(2L, "친구", FriendRelationshipStatus.FRIEND, null),
                        LocalDateTime.of(2026, 9, 14, 18, 0)
                )
        );
        HeaderDescriptor[] headers = authorizationHeaders();
        ParameterDescriptor[] pathParameters = requestIdPathParameters();
        FieldDescriptor[] responseFields = friendResponseFields();

        mockMvc.perform(post("/friends/requests/{requestId}/accept", 101L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendshipId").value(101))
                .andExpect(jsonPath("$.user.relationshipStatus").value("FRIEND"))
                .andDo(document("friends/requests-accept",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        pathParameters(pathParameters),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Accept friend request")
                                .description("로그인 사용자가 받은 대기 중인 친구 요청을 수락한다.")
                                .requestHeaders(headers)
                                .pathParameters(requestIdPathParametersForOpenApi())
                                .responseSchema(schema("FriendResponse"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("대기 중인 친구 요청을 삭제하면 204를 반환한다")
    void deletePendingRequest_returnsNoContent() throws Exception {
        HeaderDescriptor[] headers = authorizationHeaders();
        ParameterDescriptor[] pathParameters = requestIdPathParameters();

        mockMvc.perform(delete("/friends/requests/{requestId}", 101L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("friends/requests-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        pathParameters(pathParameters),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Reject received friend request")
                                .description("대기 중인 요청의 수신자만 거절한다. 발신자의 취소 및 제삼자의 삭제는 403이다. 수락된 요청은 409이며 친구 끊기 API를 사용한다.")
                                .requestHeaders(headers)
                                .pathParameters(requestIdPathParametersForOpenApi())
                                .build()
                        )));
    }

    @Test
    @DisplayName("친구 목록을 조회하면 수락된 친구 목록을 반환한다")
    void getFriends_returnsAcceptedFriends() throws Exception {
        when(friendService.getFriends(1L)).thenReturn(List.of(
                new FriendResponse(
                        201L,
                        listUserResponse(2L, "친구", FriendRelationshipStatus.FRIEND, null),
                        LocalDateTime.of(2026, 9, 14, 18, 0)
                )
        ));
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] responseFields = friendResponseListFields();

        mockMvc.perform(get("/friends").header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].friendshipId").value(201))
                .andExpect(jsonPath("$[0].user.userId").value(2))
                .andExpect(jsonPath("$[0].user.email").value("friend@example.com"))
                .andDo(document("friends/list-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("List friends")
                                .description("로그인 사용자가 수락한 친구 관계 목록을 조회한다.")
                                .requestHeaders(headers)
                                .responseSchema(schema("FriendResponseList"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 친구 목록 조회 시 401 에러를 반환한다")
    void getFriends_returnsUnauthorizedWithoutAuthorizationHeader() throws Exception {
        FieldDescriptor[] errorFields = errorResponseFields();

        mockMvc.perform(get("/friends"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andDo(document("friends/list-get-unauthorized",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(errorFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("List friends")
                                .description("로그인 사용자가 수락한 친구 관계 목록을 조회한다.")
                                .responseSchema(schema("ErrorResponse"))
                                .responseFields(errorFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("친구 관계를 끊으면 204를 반환한다")
    void unfriend_returnsNoContent() throws Exception {
        HeaderDescriptor[] headers = authorizationHeaders();
        ParameterDescriptor[] pathParameters = friendshipIdPathParameters();

        mockMvc.perform(delete("/friends/{friendshipId}", 201L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("friends/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        pathParameters(pathParameters),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Unfriend")
                                .description("현재 로그인 사용자가 참여한 친구 관계를 해제한다.")
                                .requestHeaders(headers)
                                .pathParameters(friendshipIdPathParametersForOpenApi())
                                .build()
                        )));
    }

    @ParameterizedTest(name = "{0} returns {1}")
    @MethodSource("errorCases")
    void documentedErrors(String operation, int httpStatus) throws Exception {
        ResponseStatusException error = new ResponseStatusException(HttpStatus.valueOf(httpStatus));
        MockHttpServletRequestBuilder request;
        switch (operation) {
            case "invitee-get" -> {
                when(friendService.getInvitee("invite-code", 1L)).thenThrow(error);
                request = get("/friends/invite/{code}", "invite-code");
            }
            case "search-email-get" -> {
                when(friendService.searchByEmail("friend@example.com", 1L)).thenThrow(error);
                request = get("/friends/search").param("email", "friend@example.com");
            }
            case "requests-create" -> {
                when(friendService.sendRequest(1L, 2L)).thenThrow(error);
                request = post("/friends/requests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2}");
            }
            case "requests-accept" -> {
                when(friendService.acceptRequest(101L, 1L)).thenThrow(error);
                request = post("/friends/requests/{requestId}/accept", 101L);
            }
            case "requests-delete" -> {
                doThrow(error).when(friendService).deletePendingRequest(101L, 1L);
                request = delete("/friends/requests/{requestId}", 101L);
            }
            case "delete" -> {
                doThrow(error).when(friendService).unfriend(201L, 1L);
                request = delete("/friends/{friendshipId}", 201L);
            }
            default -> throw new IllegalArgumentException(operation);
        }
        FieldDescriptor[] fields = errorResponseFields();
        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.status").value(httpStatus))
                .andDo(document("friends/" + operation + "-error-" + httpStatus,
                        preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        responseFields(fields),
                        resource(ResourceSnippetParameters.builder().tag("Friend")
                                .responseSchema(schema("ErrorResponse")).responseFields(fields).build())));
    }

    private static Stream<Arguments> errorCases() {
        return Stream.of(
                Arguments.of("invitee-get", 404),
                Arguments.of("search-email-get", 400), Arguments.of("search-email-get", 404),
                Arguments.of("requests-create", 400), Arguments.of("requests-create", 404), Arguments.of("requests-create", 409),
                Arguments.of("requests-accept", 403), Arguments.of("requests-accept", 404), Arguments.of("requests-accept", 409),
                Arguments.of("requests-delete", 403), Arguments.of("requests-delete", 404), Arguments.of("requests-delete", 409),
                Arguments.of("delete", 403), Arguments.of("delete", 404), Arguments.of("delete", 409));
    }

    @ParameterizedTest(name = "{0} requires authentication")
    @MethodSource("authenticatedEndpoints")
    void everyFriendEndpoint_requiresAuthentication(String operation, String method, String path) throws Exception {
        MockHttpServletRequestBuilder request = switch (method) {
            case "POST" -> path.equals("/friends/requests")
                    ? post(path).contentType(MediaType.APPLICATION_JSON).content("{\"targetUserId\":2}")
                    : post(path, 101L);
            case "DELETE" -> delete(path, 101L);
            default -> get(path, 101L);
        };
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andDo(document("friends/" + operation + "-authentication-error",
                        resource(ResourceSnippetParameters.builder().tag("Friend")
                                .responseSchema(schema("ErrorResponse")).responseFields(errorResponseFields()).build())));
    }

    private static Stream<Arguments> authenticatedEndpoints() {
        return Stream.of(
                Arguments.of("invite-get", "GET", "/friends/invite"),
                Arguments.of("invitee-get", "GET", "/friends/invite/{code}"),
                Arguments.of("search-email-get", "GET", "/friends/search?email=friend@example.com"),
                Arguments.of("requests-create", "POST", "/friends/requests"),
                Arguments.of("requests-sent-get", "GET", "/friends/requests/sent"),
                Arguments.of("requests-received-get", "GET", "/friends/requests/received"),
                Arguments.of("requests-accept", "POST", "/friends/requests/{requestId}/accept"),
                Arguments.of("requests-delete", "DELETE", "/friends/requests/{requestId}"),
                Arguments.of("list-get", "GET", "/friends"),
                Arguments.of("delete", "DELETE", "/friends/{friendshipId}"));
    }

    private FriendUserResponse userResponse(
            Long userId,
            String displayName,
            FriendRelationshipStatus relationshipStatus,
            Long requestId
    ) {
        return new FriendUserResponse(userId, displayName, null, relationshipStatus, requestId);
    }

    private FriendListUserResponse listUserResponse(Long userId, String name,
                                                   FriendRelationshipStatus status, Long requestId) {
        return new FriendListUserResponse(userId, name, null, status, requestId, "friend@example.com");
    }

    private FriendRequestResponse requestResponse(
            Long requestId,
            Long userId,
            String displayName,
            FriendRelationshipStatus relationshipStatus,
            Long nestedRequestId
    ) {
        return new FriendRequestResponse(
                requestId,
                userResponse(userId, displayName, relationshipStatus, nestedRequestId),
                LocalDateTime.of(2026, 9, 14, 17, 0)
        );
    }

    private HeaderDescriptor[] authorizationHeaders() {
        return new HeaderDescriptor[] {
                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private ParameterDescriptor[] codePathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("code").description("친구 초대코드")
        };
    }

    private ParameterDescriptor[] emailQueryParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("email").description("정확히 검색할 사용자 이메일")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] emailQueryParametersForOpenApi() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("email")
                        .type(SimpleType.STRING)
                        .description("정확히 검색할 사용자 이메일")
        };
    }

    private ParameterDescriptor[] requestIdPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("requestId").description("친구 요청 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] requestIdPathParametersForOpenApi() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("requestId")
                        .type(SimpleType.INTEGER)
                        .description("친구 요청 ID")
        };
    }

    private ParameterDescriptor[] friendshipIdPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("friendshipId").description("친구 관계 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] friendshipIdPathParametersForOpenApi() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("friendshipId")
                        .type(SimpleType.INTEGER)
                        .description("친구 관계 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] codePathParametersForOpenApi() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("code")
                        .type(SimpleType.STRING)
                        .description("친구 초대코드")
        };
    }

    private FieldDescriptor[] inviteResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("code").type(JsonFieldType.STRING).description("자동 만료되지 않는 고정 랜덤 초대코드"),
                fieldWithPath("email").type(JsonFieldType.STRING).optional().description("공유용 본인 이메일. 없는 기존 계정은 null")
        };
    }

    private FieldDescriptor[] createRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("targetUserId").type(JsonFieldType.NUMBER).description("친구 요청을 보낼 대상 사용자 ID")
        };
    }

    private FieldDescriptor[] userResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("displayName").type(JsonFieldType.STRING).description("사용자 공개 닉네임"),
                fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).optional().description("프로필 이미지 읽기 URL"),
                relationshipField("relationshipStatus"),
                fieldWithPath("requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID")
        };
    }

    private FieldDescriptor[] requestResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("requestId").type(JsonFieldType.NUMBER).description("친구 요청 ID"),
                fieldWithPath("user.userId").type(JsonFieldType.NUMBER).description("상대 사용자 ID"),
                fieldWithPath("user.displayName").type(JsonFieldType.STRING).description("상대 사용자 공개 닉네임"),
                fieldWithPath("user.profileImageUrl").type(JsonFieldType.STRING).optional().description("상대 사용자 프로필 이미지 읽기 URL"),
                relationshipField("user.relationshipStatus"),
                fieldWithPath("user.requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID"),
                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("친구 요청 생성 시각")
        };
    }

    private FieldDescriptor[] inviteeResponseFields() {
        return Stream.concat(Arrays.stream(userResponseFields()), Stream.of(
                integerField("daysSinceStart", 1, "가입 당일 1일부터 시작하는 경과일 (Asia/Seoul), 64비트 정수"),
                integerField("targetSuccessCount", 0, "모든 그룹·기간의 성공한 챌린지 기록 수, 64비트 정수. 같은 날짜의 여러 성공 기록도 각각 합산")
        )).toArray(FieldDescriptor[]::new);
    }

    private FieldDescriptor[] searchResponseFields() {
        return Stream.concat(Arrays.stream(userResponseFields()), Stream.of(
                integerField("mutualFriendCount", 0, "활성 공통 친구 수, 64비트 정수. 자기 자신은 0"),
                fieldWithPath("mutualFriendPreviewName").type(JsonFieldType.STRING).optional().description("가장 작은 사용자 ID의 공통 친구 이름. 0명 또는 자기 자신이면 null")
        )).toArray(FieldDescriptor[]::new);
    }

    private FieldDescriptor[] receivedRequestResponseListFields() {
        return Stream.concat(Arrays.stream(requestResponseListFields()), Stream.of(
                fieldWithPath("[].user.email").type(JsonFieldType.STRING).optional().description("요청자의 이메일. 없는 기존 계정은 null")
        )).toArray(FieldDescriptor[]::new);
    }

    private FieldDescriptor[] requestResponseListFields() {
        return new FieldDescriptor[] {
                fieldWithPath("[].requestId").type(JsonFieldType.NUMBER).description("친구 요청 ID"),
                fieldWithPath("[].user.userId").type(JsonFieldType.NUMBER).description("상대 사용자 ID"),
                fieldWithPath("[].user.displayName").type(JsonFieldType.STRING).description("상대 사용자 공개 닉네임"),
                fieldWithPath("[].user.profileImageUrl").type(JsonFieldType.STRING).optional().description("상대 사용자 프로필 이미지 읽기 URL"),
                relationshipField("[].user.relationshipStatus"),
                fieldWithPath("[].user.requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID"),
                fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("친구 요청 생성 시각")
        };
    }

    private FieldDescriptor[] friendResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("friendshipId").type(JsonFieldType.NUMBER).description("친구 관계 ID"),
                fieldWithPath("user.userId").type(JsonFieldType.NUMBER).description("친구 사용자 ID"),
                fieldWithPath("user.displayName").type(JsonFieldType.STRING).description("친구 사용자 공개 닉네임"),
                fieldWithPath("user.email").type(JsonFieldType.STRING).optional().description("친구 이메일. 이메일이 없거나 탈퇴한 계정은 null"),
                fieldWithPath("user.profileImageUrl").type(JsonFieldType.STRING).optional().description("친구 사용자 프로필 이미지 읽기 URL"),
                relationshipField("user.relationshipStatus"),
                fieldWithPath("user.requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID"),
                fieldWithPath("acceptedAt").type(JsonFieldType.STRING).description("친구 관계 수락 시각")
        };
    }

    private FieldDescriptor[] friendResponseListFields() {
        return new FieldDescriptor[] {
                fieldWithPath("[].friendshipId").type(JsonFieldType.NUMBER).description("친구 관계 ID"),
                fieldWithPath("[].user.userId").type(JsonFieldType.NUMBER).description("친구 사용자 ID"),
                fieldWithPath("[].user.displayName").type(JsonFieldType.STRING).description("친구 사용자 공개 닉네임"),
                fieldWithPath("[].user.email").type(JsonFieldType.STRING).optional().description("친구 이메일. 이메일이 없거나 탈퇴한 계정은 null"),
                fieldWithPath("[].user.profileImageUrl").type(JsonFieldType.STRING).optional().description("친구 사용자 프로필 이미지 읽기 URL"),
                relationshipField("[].user.relationshipStatus"),
                fieldWithPath("[].user.requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID"),
                fieldWithPath("[].acceptedAt").type(JsonFieldType.STRING).description("친구 관계 수락 시각")
        };
    }

    private FieldDescriptor relationshipField(String path) {
        return new EnumFields(FriendRelationshipStatus.class).withPath(path)
                .description("현재 사용자와의 관계 상태 (NONE | SELF | PENDING_SENT | PENDING_RECEIVED | FRIEND)");
    }

    private FieldDescriptor integerField(String path, int minimum, String description) {
        // restdocs-api-spec 0.20 recognizes integer minima through this legacy constraint name.
        return fieldWithPath(path).type(JsonFieldType.NUMBER).description(description)
                .attributes(key("validationConstraints").value(List.of(
                        new Constraint("javax.validation.constraints.Min", Map.of("value", minimum))
                )));
    }

    private FieldDescriptor[] errorResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드")
        };
    }
}
