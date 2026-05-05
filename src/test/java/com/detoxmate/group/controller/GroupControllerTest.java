package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
import com.detoxmate.group.service.GroupService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(RestDocumentationExtension.class)
class GroupControllerTest {

    private GroupService groupService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        groupService = mock(GroupService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://..."));
        mockMvc = MockMvcBuilders.standaloneSetup(new GroupController(groupService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 그룹_생성_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "name": "주말 디톡스"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 그룹을_생성하면_생성된_그룹_정보를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = createGroupRequestFields();
        FieldDescriptor[] responseFieldDescriptors = groupResponseFields();
        when(groupService.createGroup(1L, "주말 디톡스"))
                .thenReturn(GroupMockData.createGroupResponse("주말 디톡스"));

        mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "name": "주말 디톡스"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("주말 디톡스"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.currentChallenge.status").value("RECRUITING"))
                .andDo(document("groups/create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Create group")
                                .description("그룹을 생성하고 생성자를 첫 멤버 및 첫 챌린지 참가자로 등록한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("CreateGroupRequest"))
                                .responseSchema(schema("GroupResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_생성_요청의_이름이_12자를_초과하면_400_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "name": "1234567890123"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void 초대코드로_그룹에_참여하면_업데이트된_그룹_정보를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = joinGroupRequestFields();
        FieldDescriptor[] responseFieldDescriptors = groupResponseFields();
        when(groupService.joinGroup("AB123", 1L))
                .thenReturn(GroupMockData.joinGroupResponse("AB123"));

        mockMvc.perform(post("/groups/join")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "inviteCode": "AB123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inviteCode").value("AB123"))
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.members[1].displayName").value("민수"))
                .andDo(document("groups/join",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Join group")
                                .description("초대코드로 그룹에 참여하고 현재 챌린지가 모집 중이면 자동 참가한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("JoinGroupRequest"))
                                .responseSchema(schema("GroupResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_참여_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/groups/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "inviteCode": "AB123"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 존재하지_않는_초대코드로_그룹에_참여하면_404_에러를_반환한다() throws Exception {
        when(groupService.joinGroup("ZZZZZ", 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "초대코드에 해당하는 그룹이 없습니다."));

        mockMvc.perform(post("/groups/join")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "inviteCode": "ZZZZZ"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void 이미_그룹이_있는_유저가_그룹을_생성하면_409_에러를_반환한다() throws Exception {
        when(groupService.createGroup(1L, "주말 디톡스"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "이미 그룹이 있어서, 새로운 그룹을 생성할 수 없습니다."));

        mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "name": "주말 디톡스"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void 내_그룹_목록을_조회하면_그룹_배열을_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] responseFieldDescriptors = groupListResponseFields();
        when(groupService.getMyGroups(1L))
                .thenReturn(GroupMockData.myGroupsResponse());

        mockMvc.perform(get("/me/groups")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].currentChallenge.status").value("ACTIVE"))
                .andDo(result -> verify(groupService).getMyGroups(1L))
                .andDo(document("me/groups-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Get my groups")
                                .description("내가 속한 그룹 목록을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .responseSchema(schema("MyGroupsResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 내_그룹_목록_조회_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(get("/me/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 그룹_상세를_조회하면_그룹_정보를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] responseFieldDescriptors = groupResponseFields();
        ParameterDescriptor[] pathParameterDescriptors = groupIdPathParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedPathParameterDescriptors = groupIdOpenApiPathParameters();
        when(groupService.getGroup(1L, 1L))
                .thenReturn(GroupMockData.groupDetailResponse(1L));

        mockMvc.perform(get("/groups/{groupId}", 1L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.members[1].role").value("MEMBER"))
                .andDo(result -> verify(groupService).getGroup(1L, 1L))
                .andDo(document("groups/get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Get group detail")
                                .description("그룹 상세 정보를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(typedPathParameterDescriptors)
                                .responseSchema(schema("GroupResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_상세_조회_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(get("/groups/{groupId}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 그룹_이름을_변경하면_변경된_그룹_정보를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = updateGroupRequestFields();
        FieldDescriptor[] responseFieldDescriptors = groupResponseFields();
        ParameterDescriptor[] pathParameterDescriptors = groupIdPathParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedPathParameterDescriptors = groupIdOpenApiPathParameters();
        when(groupService.updateGroup(1L, 1L, "주말 디톡스"))
                .thenReturn(GroupMockData.groupDetailResponse(1L));

        mockMvc.perform(patch("/groups/{groupId}", 1L)
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "name": "주말 디톡스"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("주말 디톡스"))
                .andDo(result -> verify(groupService).updateGroup(1L, 1L, "주말 디톡스"))
                .andDo(document("groups/patch",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Update group")
                                .description("로그인 사용자가 속한 그룹의 전역 그룹 이름을 변경한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(typedPathParameterDescriptors)
                                .requestSchema(schema("UpdateGroupRequest"))
                                .responseSchema(schema("GroupResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹을_탈퇴하면_204_응답을_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = groupIdPathParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedPathParameterDescriptors = groupIdOpenApiPathParameters();

        mockMvc.perform(delete("/groups/{groupId}/members/me", 1L)
                .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(result -> verify(groupService).withdrawGroup(1L, 1L))
                .andDo(document("groups/members/me-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Leave group")
                                .description("현재 사용자가 그룹을 탈퇴하고 최신 챌린지 참가 상태가 있으면 함께 이탈 처리한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(typedPathParameterDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_탈퇴_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(delete("/groups/{groupId}/members/me", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    private ParameterDescriptor[] groupIdPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("groupId").description("그룹 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] groupIdOpenApiPathParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("groupId")
                        .type(SimpleType.INTEGER)
                        .description("그룹 ID")
        };
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] createGroupRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("name")
                        .type(JsonFieldType.STRING)
                        .description("생성할 그룹 이름")
        };
    }

    private FieldDescriptor[] joinGroupRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("inviteCode")
                        .type(JsonFieldType.STRING)
                        .description("참여할 그룹의 초대 코드")
        };
    }

    private FieldDescriptor[] updateGroupRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("name")
                        .type(JsonFieldType.STRING)
                        .description("변경할 그룹 이름. 공백 포함 12자 이내")
        };
    }

    private FieldDescriptor[] groupResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("inviteCode").type(JsonFieldType.STRING).description("그룹 초대 코드"),
                fieldWithPath("name").type(JsonFieldType.STRING).description("그룹 이름"),
                fieldWithPath("myRole").type(JsonFieldType.STRING).description("현재 사용자의 그룹 내 역할"),
                fieldWithPath("members").type(JsonFieldType.ARRAY).description("그룹 멤버 목록"),
                fieldWithPath("members[].id").type(JsonFieldType.NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("members[].userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("members[].displayName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                fieldWithPath("members[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("members[].role").type(JsonFieldType.STRING).description("그룹 멤버 역할"),
                fieldWithPath("members[].status").type(JsonFieldType.STRING).description("그룹 멤버 상태"),
                fieldWithPath("members[].joinedAt").type(JsonFieldType.STRING).description("그룹 참여 일시"),
                fieldWithPath("members[].leftAt").type(JsonFieldType.STRING).description("그룹 이탈 일시").optional(),
                fieldWithPath("currentChallenge").type(JsonFieldType.OBJECT).description("현재 진행 중이거나 모집 중인 챌린지"),
                fieldWithPath("currentChallenge.id").type(JsonFieldType.NUMBER).description("현재 챌린지 ID"),
                fieldWithPath("currentChallenge.challengeNo").type(JsonFieldType.NUMBER).description("그룹 내 챌린지 순번"),
                fieldWithPath("currentChallenge.status").type(JsonFieldType.STRING).description("현재 챌린지 상태"),
                fieldWithPath("currentChallenge.startAt").type(JsonFieldType.STRING).description("챌린지 시작 일시").optional(),
                fieldWithPath("currentChallenge.endAt").type(JsonFieldType.STRING).description("챌린지 종료 일시").optional(),
                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("그룹 생성 일시"),
                fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("그룹 수정 일시")
        };
    }

    private FieldDescriptor[] groupListResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("[].inviteCode").type(JsonFieldType.STRING).description("그룹 초대 코드"),
                fieldWithPath("[].name").type(JsonFieldType.STRING).description("그룹 이름"),
                fieldWithPath("[].myRole").type(JsonFieldType.STRING).description("현재 사용자의 그룹 내 역할"),
                fieldWithPath("[].members").type(JsonFieldType.ARRAY).description("그룹 멤버 목록"),
                fieldWithPath("[].members[].id").type(JsonFieldType.NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("[].members[].userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("[].members[].displayName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                fieldWithPath("[].members[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("[].members[].role").type(JsonFieldType.STRING).description("그룹 멤버 역할"),
                fieldWithPath("[].members[].status").type(JsonFieldType.STRING).description("그룹 멤버 상태"),
                fieldWithPath("[].members[].joinedAt").type(JsonFieldType.STRING).description("그룹 참여 일시"),
                fieldWithPath("[].members[].leftAt").type(JsonFieldType.STRING).description("그룹 이탈 일시").optional(),
                fieldWithPath("[].currentChallenge").type(JsonFieldType.OBJECT).description("현재 진행 중인 챌린지"),
                fieldWithPath("[].currentChallenge.id").type(JsonFieldType.NUMBER).description("현재 챌린지 ID"),
                fieldWithPath("[].currentChallenge.challengeNo").type(JsonFieldType.NUMBER).description("그룹 내 챌린지 순번"),
                fieldWithPath("[].currentChallenge.status").type(JsonFieldType.STRING).description("현재 챌린지 상태"),
                fieldWithPath("[].currentChallenge.startAt").type(JsonFieldType.STRING).description("챌린지 시작 일시").optional(),
                fieldWithPath("[].currentChallenge.endAt").type(JsonFieldType.STRING).description("챌린지 종료 일시").optional(),
                fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("그룹 생성 일시"),
                fieldWithPath("[].updatedAt").type(JsonFieldType.STRING).description("그룹 수정 일시")
        };
    }
}
