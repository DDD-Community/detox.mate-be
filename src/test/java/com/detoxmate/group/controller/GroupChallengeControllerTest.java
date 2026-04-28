package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
import com.detoxmate.group.service.GroupChallengeService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(RestDocumentationExtension.class)
class GroupChallengeControllerTest {

    private GroupChallengeService groupChallengeService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        groupChallengeService = mock(GroupChallengeService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://..."));
        mockMvc = MockMvcBuilders.standaloneSetup(new GroupChallengeController(groupChallengeService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 내_그룹_챌린지_목록을_조회하면_챌린지_배열을_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] responseFieldDescriptors = groupChallengeListResponseFields();
        ParameterDescriptor[] queryParameterDescriptors = groupChallengeListQueryParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedQueryParameterDescriptors = groupChallengeListOpenApiQueryParameters();
        when(groupChallengeService.getMyGroupChallenges(1L, "ACTIVE"))
                .thenReturn(GroupMockData.myGroupChallengesResponse("ACTIVE"));

        mockMvc.perform(get("/me/group-challenges")
                        .header("Authorization", "Bearer access-token")
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].participants[0].goalTimes").isArray())
                .andExpect(jsonPath("$[0].participants[0].goalTimes").isEmpty())
                .andDo(result -> verify(groupChallengeService).getMyGroupChallenges(1L, "ACTIVE"))
                .andDo(document("me/group-challenges-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        queryParameters(queryParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group Challenge")
                                .summary("Get my group challenges")
                                .description("내가 참여한 그룹 챌린지 목록을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .queryParameters(typedQueryParameterDescriptors)
                                .responseSchema(schema("MyGroupChallengesResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 상태값_없이_내_그룹_챌린지_목록을_조회하면_전체_목록을_반환한다() throws Exception {
        when(groupChallengeService.getMyGroupChallenges(1L, null))
                .thenReturn(GroupMockData.myGroupChallengesResponse(null));

        mockMvc.perform(get("/me/group-challenges")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andDo(result -> verify(groupChallengeService).getMyGroupChallenges(1L, null));
    }

    @Test
    void 잘못된_상태값으로_내_그룹_챌린지_목록을_조회하면_400_에러를_반환한다() throws Exception {
        when(groupChallengeService.getMyGroupChallenges(1L, "WRONG"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 상태값입니다."));

        mockMvc.perform(get("/me/group-challenges")
                        .header("Authorization", "Bearer access-token")
                        .queryParam("status", "WRONG"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void 내_그룹_챌린지_목록_조회_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(get("/me/group-challenges"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 그룹_챌린지_상세를_조회하면_챌린지_정보를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] responseFieldDescriptors = groupChallengeResponseFields();
        ParameterDescriptor[] pathParameterDescriptors = groupChallengeIdPathParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedPathParameterDescriptors = groupChallengeIdOpenApiPathParameters();
        when(groupChallengeService.getGroupChallenge(10L, 1L))
                .thenReturn(GroupMockData.groupChallengeDetailResponse(10L));

        mockMvc.perform(get("/group-challenges/{id}", 10L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.participants[1].displayName").value("민수"))
                .andExpect(jsonPath("$.participants[0].goalTimes").isArray())
                .andExpect(jsonPath("$.participants[0].goalTimes").isEmpty())
                .andDo(result -> verify(groupChallengeService).getGroupChallenge(10L, 1L))
                .andDo(document("group-challenges/get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group Challenge")
                                .summary("Get group challenge detail")
                                .description("그룹 챌린지 상세 정보를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(typedPathParameterDescriptors)
                                .responseSchema(schema("GroupChallengeResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_챌린지_상세_조회_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(get("/group-challenges/{id}", 10L))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    private ParameterDescriptor[] groupChallengeListQueryParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("status")
                        .description("조회할 그룹 챌린지 상태")
                        .optional()
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] groupChallengeListOpenApiQueryParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("status")
                        .type(SimpleType.STRING)
                        .optional()
                        .description("조회할 그룹 챌린지 상태")
        };
    }

    private ParameterDescriptor[] groupChallengeIdPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("id").description("조회할 그룹 챌린지 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] groupChallengeIdOpenApiPathParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("id")
                        .type(SimpleType.INTEGER)
                        .description("조회할 그룹 챌린지 ID")
        };
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] groupChallengeResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("그룹 챌린지 ID"),
                fieldWithPath("groupId").type(JsonFieldType.NUMBER).description("소속 그룹 ID"),
                fieldWithPath("groupName").type(JsonFieldType.STRING).description("그룹 이름"),
                fieldWithPath("challengeNo").type(JsonFieldType.NUMBER).description("그룹 내 챌린지 순번"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("그룹 챌린지 상태"),
                fieldWithPath("participants").type(JsonFieldType.ARRAY).description("그룹 챌린지 참가자 목록"),
                fieldWithPath("participants[].id").type(JsonFieldType.NUMBER).description("참가자 ID"),
                fieldWithPath("participants[].groupMemberId").type(JsonFieldType.NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("participants[].userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("participants[].displayName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                fieldWithPath("participants[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("participants[].status").type(JsonFieldType.STRING).description("참가 상태"),
                fieldWithPath("participants[].joinedAt").type(JsonFieldType.STRING).description("챌린지 참여 일시"),
                fieldWithPath("participants[].withdrawnAt").type(JsonFieldType.STRING).description("챌린지 이탈 일시").optional(),
                fieldWithPath("participants[].goalTimes").type(JsonFieldType.ARRAY).description("참가자별 이번 챌린지 앱 사용 목표 시간 스냅샷 목록. 아직 설정하지 않았으면 빈 배열을 반환한다."),
                fieldWithPath("participants[].goalTimes[].type").type(JsonFieldType.STRING).description("앱 사용 목표 타입(INSTAGRAM, YOUTUBE, TOTAL_USAGE)").optional(),
                fieldWithPath("participants[].goalTimes[].minutes").type(JsonFieldType.NUMBER).description("이번 챌린지에 적용되는 앱 사용 목표 시간(분 단위 정수)").optional(),
                fieldWithPath("startAt").type(JsonFieldType.STRING).description("챌린지 시작 일시").optional(),
                fieldWithPath("endAt").type(JsonFieldType.STRING).description("챌린지 종료 일시").optional(),
                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("챌린지 생성 일시"),
                fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("챌린지 수정 일시")
        };
    }

    private FieldDescriptor[] groupChallengeListResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("그룹 챌린지 ID"),
                fieldWithPath("[].groupId").type(JsonFieldType.NUMBER).description("소속 그룹 ID"),
                fieldWithPath("[].groupName").type(JsonFieldType.STRING).description("그룹 이름"),
                fieldWithPath("[].challengeNo").type(JsonFieldType.NUMBER).description("그룹 내 챌린지 순번"),
                fieldWithPath("[].status").type(JsonFieldType.STRING).description("그룹 챌린지 상태"),
                fieldWithPath("[].participants").type(JsonFieldType.ARRAY).description("그룹 챌린지 참가자 목록"),
                fieldWithPath("[].participants[].id").type(JsonFieldType.NUMBER).description("참가자 ID"),
                fieldWithPath("[].participants[].groupMemberId").type(JsonFieldType.NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("[].participants[].userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("[].participants[].displayName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                fieldWithPath("[].participants[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("[].participants[].status").type(JsonFieldType.STRING).description("참가 상태"),
                fieldWithPath("[].participants[].joinedAt").type(JsonFieldType.STRING).description("챌린지 참여 일시"),
                fieldWithPath("[].participants[].withdrawnAt").type(JsonFieldType.STRING).description("챌린지 이탈 일시").optional(),
                fieldWithPath("[].participants[].goalTimes").type(JsonFieldType.ARRAY).description("참가자별 이번 챌린지 앱 사용 목표 시간 스냅샷 목록. 아직 설정하지 않았으면 빈 배열을 반환한다."),
                fieldWithPath("[].participants[].goalTimes[].type").type(JsonFieldType.STRING).description("앱 사용 목표 타입(INSTAGRAM, YOUTUBE, TOTAL_USAGE)").optional(),
                fieldWithPath("[].participants[].goalTimes[].minutes").type(JsonFieldType.NUMBER).description("이번 챌린지에 적용되는 앱 사용 목표 시간(분 단위 정수)").optional(),
                fieldWithPath("[].startAt").type(JsonFieldType.STRING).description("챌린지 시작 일시").optional(),
                fieldWithPath("[].endAt").type(JsonFieldType.STRING).description("챌린지 종료 일시").optional(),
                fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("챌린지 생성 일시"),
                fieldWithPath("[].updatedAt").type(JsonFieldType.STRING).description("챌린지 수정 일시")
        };
    }
}
