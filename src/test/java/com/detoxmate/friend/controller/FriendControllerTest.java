package com.detoxmate.friend.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.friend.dto.FriendInviteResponse;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.dto.FriendRequestResponse;
import com.detoxmate.friend.dto.FriendResponse;
import com.detoxmate.friend.dto.FriendUserResponse;
import com.detoxmate.friend.service.FriendService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
        when(friendService.getMyInvite(1L)).thenReturn(new FriendInviteResponse("a".repeat(64)));
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] responseFields = inviteResponseFields();

        mockMvc.perform(get("/friends/invite").header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("a".repeat(64)))
                .andDo(document("friends/invite-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Get my friend invite code")
                                .description("로그인 사용자의 친구 초대코드를 조회한다. 초대코드가 없으면 새로 생성하고 이후에는 같은 코드를 반환한다.")
                                .requestHeaders(headers)
                                .responseSchema(schema("FriendInviteResponse"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("초대코드로 친구를 조회하면 친구 공개 정보와 관계 상태를 반환한다")
    void getInvitee_returnsInvitedUser() throws Exception {
        FriendUserResponse response = userResponse(2L, "친구", FriendRelationshipStatus.NONE, null);
        when(friendService.getInvitee("invite-code", 1L)).thenReturn(response);
        HeaderDescriptor[] headers = authorizationHeaders();
        ParameterDescriptor[] pathParameters = codePathParameters();
        FieldDescriptor[] responseFields = userResponseFields();

        mockMvc.perform(get("/friends/invite/{code}", "invite-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.relationshipStatus").value("NONE"))
                .andDo(document("friends/invitee-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        pathParameters(pathParameters),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Get user by friend invite code")
                                .description("친구 초대코드에 해당하는 활성 사용자의 공개 정보와 현재 로그인 사용자와의 친구 관계 상태를 조회한다.")
                                .requestHeaders(headers)
                                .pathParameters(codePathParametersForOpenApi())
                                .responseSchema(schema("FriendUserResponse"))
                                .responseFields(responseFields)
                                .build()
                        )));
    }

    @Test
    @DisplayName("이메일로 친구를 검색하면 정확히 일치하는 사용자를 반환한다")
    void searchByEmail_returnsExactMatch() throws Exception {
        when(friendService.searchByEmail("friend@example.com", 1L))
                .thenReturn(userResponse(2L, "친구", FriendRelationshipStatus.NONE, null));
        HeaderDescriptor[] headers = authorizationHeaders();
        ParameterDescriptor[] queryParameters = emailQueryParameters();
        FieldDescriptor[] responseFields = userResponseFields();

        mockMvc.perform(get("/friends/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .param("email", "friend@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.relationshipStatus").value("NONE"))
                .andDo(document("friends/search-email-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headers),
                        queryParameters(queryParameters),
                        responseFields(responseFields),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Friend")
                                .summary("Search friend by exact email")
                                .description("이메일이 정규화된 값과 정확히 일치하는 활성 사용자를 조회한다. 이메일은 응답에 포함하지 않는다.")
                                .requestHeaders(headers)
                                .queryParameters(emailQueryParametersForOpenApi())
                                .responseSchema(schema("FriendUserResponse"))
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
                requestResponse(101L, 2L, "친구", FriendRelationshipStatus.PENDING_RECEIVED, 101L)
        ));
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] responseFields = requestResponseListFields();

        mockMvc.perform(get("/friends/requests/received")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(101))
                .andExpect(jsonPath("$[0].user.relationshipStatus").value("PENDING_RECEIVED"))
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
                                .responseSchema(schema("FriendRequestResponseList"))
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
                        userResponse(2L, "친구", FriendRelationshipStatus.FRIEND, null),
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
                                .summary("Cancel or reject friend request")
                                .description("친구 요청의 발신자는 취소하고 수신자는 거절한다.")
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
                        userResponse(2L, "친구", FriendRelationshipStatus.FRIEND, null),
                        LocalDateTime.of(2026, 9, 14, 18, 0)
                )
        ));
        HeaderDescriptor[] headers = authorizationHeaders();
        FieldDescriptor[] responseFields = friendResponseListFields();

        mockMvc.perform(get("/friends").header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].friendshipId").value(201))
                .andExpect(jsonPath("$[0].user.userId").value(2))
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

    private FriendUserResponse userResponse(
            Long userId,
            String displayName,
            FriendRelationshipStatus relationshipStatus,
            Long requestId
    ) {
        return new FriendUserResponse(userId, displayName, null, relationshipStatus, requestId);
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
                fieldWithPath("code").type(JsonFieldType.STRING).description("친구 초대코드")
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
                fieldWithPath("relationshipStatus").type(JsonFieldType.STRING).description("현재 사용자와의 관계 상태 (NONE | SELF | PENDING_SENT | PENDING_RECEIVED | FRIEND)"),
                fieldWithPath("requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID")
        };
    }

    private FieldDescriptor[] requestResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("requestId").type(JsonFieldType.NUMBER).description("친구 요청 ID"),
                fieldWithPath("user.userId").type(JsonFieldType.NUMBER).description("상대 사용자 ID"),
                fieldWithPath("user.displayName").type(JsonFieldType.STRING).description("상대 사용자 공개 닉네임"),
                fieldWithPath("user.profileImageUrl").type(JsonFieldType.STRING).optional().description("상대 사용자 프로필 이미지 읽기 URL"),
                fieldWithPath("user.relationshipStatus").type(JsonFieldType.STRING).description("상대 사용자와의 관계 상태"),
                fieldWithPath("user.requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID"),
                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("친구 요청 생성 시각")
        };
    }

    private FieldDescriptor[] requestResponseListFields() {
        return new FieldDescriptor[] {
                fieldWithPath("[].requestId").type(JsonFieldType.NUMBER).description("친구 요청 ID"),
                fieldWithPath("[].user.userId").type(JsonFieldType.NUMBER).description("상대 사용자 ID"),
                fieldWithPath("[].user.displayName").type(JsonFieldType.STRING).description("상대 사용자 공개 닉네임"),
                fieldWithPath("[].user.profileImageUrl").type(JsonFieldType.STRING).optional().description("상대 사용자 프로필 이미지 읽기 URL"),
                fieldWithPath("[].user.relationshipStatus").type(JsonFieldType.STRING).description("상대 사용자와의 관계 상태"),
                fieldWithPath("[].user.requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID"),
                fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("친구 요청 생성 시각")
        };
    }

    private FieldDescriptor[] friendResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("friendshipId").type(JsonFieldType.NUMBER).description("친구 관계 ID"),
                fieldWithPath("user.userId").type(JsonFieldType.NUMBER).description("친구 사용자 ID"),
                fieldWithPath("user.displayName").type(JsonFieldType.STRING).description("친구 사용자 공개 닉네임"),
                fieldWithPath("user.profileImageUrl").type(JsonFieldType.STRING).optional().description("친구 사용자 프로필 이미지 읽기 URL"),
                fieldWithPath("user.relationshipStatus").type(JsonFieldType.STRING).description("친구 관계 상태"),
                fieldWithPath("user.requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID"),
                fieldWithPath("acceptedAt").type(JsonFieldType.STRING).description("친구 관계 수락 시각")
        };
    }

    private FieldDescriptor[] friendResponseListFields() {
        return new FieldDescriptor[] {
                fieldWithPath("[].friendshipId").type(JsonFieldType.NUMBER).description("친구 관계 ID"),
                fieldWithPath("[].user.userId").type(JsonFieldType.NUMBER).description("친구 사용자 ID"),
                fieldWithPath("[].user.displayName").type(JsonFieldType.STRING).description("친구 사용자 공개 닉네임"),
                fieldWithPath("[].user.profileImageUrl").type(JsonFieldType.STRING).optional().description("친구 사용자 프로필 이미지 읽기 URL"),
                fieldWithPath("[].user.relationshipStatus").type(JsonFieldType.STRING).description("친구 관계 상태"),
                fieldWithPath("[].user.requestId").type(JsonFieldType.NUMBER).optional().description("대기 중인 친구 요청 ID"),
                fieldWithPath("[].acceptedAt").type(JsonFieldType.STRING).description("친구 관계 수락 시각")
        };
    }

    private FieldDescriptor[] errorResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드")
        };
    }
}
