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
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
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
        FieldDescriptor[] requestFieldDescriptors = createGroupRequestFields();
        FieldDescriptor[] responseFieldDescriptors = groupResponseFields();
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://..."));
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
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Create group")
                                .description("그룹을 생성하고 생성자를 첫 멤버 및 첫 챌린지 참가자로 등록한다.")
                                .requestSchema(schema("CreateGroupRequest"))
                                .responseSchema(schema("GroupResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 초대코드로_그룹에_참여하면_업데이트된_그룹_정보를_반환한다() throws Exception {
        FieldDescriptor[] requestFieldDescriptors = joinGroupRequestFields();
        FieldDescriptor[] responseFieldDescriptors = groupResponseFields();

        mockMvc.perform(post("/groups/join")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "inviteCode": "AB12CD34"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inviteCode").value("AB12CD34"))
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.members[1].displayName").value("민수"))
                .andDo(document("groups/join",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Join group")
                                .description("초대코드로 그룹에 참여하고 현재 챌린지가 모집 중이면 자동 참가한다.")
                                .requestSchema(schema("JoinGroupRequest"))
                                .responseSchema(schema("GroupResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 내_그룹_목록을_조회하면_그룹_배열을_반환한다() throws Exception {
        FieldDescriptor[] responseFieldDescriptors = groupListResponseFields();

        mockMvc.perform(get("/me/groups")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].currentChallenge.status").value("ACTIVE"))
                .andDo(document("me/groups-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Get my groups")
                                .description("내가 속한 그룹 목록을 조회한다.")
                                .responseSchema(schema("MyGroupsResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_상세를_조회하면_그룹_정보를_반환한다() throws Exception {
        FieldDescriptor[] responseFieldDescriptors = groupResponseFields();
        ParameterDescriptor[] pathParameterDescriptors = groupIdPathParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedPathParameterDescriptors = groupIdOpenApiPathParameters();

        mockMvc.perform(get("/groups/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.members[1].role").value("MEMBER"))
                .andDo(document("groups/get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group")
                                .summary("Get group detail")
                                .description("그룹 상세 정보를 조회한다.")
                                .pathParameters(typedPathParameterDescriptors)
                                .responseSchema(schema("GroupResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    private ParameterDescriptor[] groupIdPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("id").description("조회할 그룹 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] groupIdOpenApiPathParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("id")
                        .type(SimpleType.INTEGER)
                        .description("조회할 그룹 ID")
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
