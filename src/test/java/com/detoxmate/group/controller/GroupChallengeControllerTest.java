package com.detoxmate.group.controller;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

@ExtendWith(RestDocumentationExtension.class)
class GroupChallengeControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.standaloneSetup(new GroupChallengeController())
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 내_그룹_챌린지_목록을_조회하면_챌린지_배열을_반환한다() throws Exception {
        FieldDescriptor[] responseFieldDescriptors = groupChallengeListResponseFields();
        ParameterDescriptor[] queryParameterDescriptors = groupChallengeListQueryParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedQueryParameterDescriptors = groupChallengeListOpenApiQueryParameters();

        mockMvc.perform(get("/me/group-challenges")
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].participants[0].goalTimes[0].type").value("INSTAGRAM"))
                .andExpect(jsonPath("$[0].participants[0].goalTimes[0].minutes").value(30))
                .andExpect(jsonPath("$[0].participants[0].goalTimes[1].type").value("YOUTUBE"))
                .andExpect(jsonPath("$[0].participants[0].goalTimes[1].minutes").value(60))
                .andDo(document("me/group-challenges-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(queryParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group Challenge")
                                .summary("Get my group challenges")
                                .description("내가 참여한 그룹 챌린지 목록을 조회한다.")
                                .queryParameters(typedQueryParameterDescriptors)
                                .responseSchema(schema("MyGroupChallengesResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_챌린지_상세를_조회하면_챌린지_정보를_반환한다() throws Exception {
        FieldDescriptor[] responseFieldDescriptors = groupChallengeResponseFields();
        ParameterDescriptor[] pathParameterDescriptors = groupChallengeIdPathParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedPathParameterDescriptors = groupChallengeIdOpenApiPathParameters();

        mockMvc.perform(get("/group-challenges/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.participants[1].displayName").value("민수"))
                .andDo(document("group-challenges/get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group Challenge")
                                .summary("Get group challenge detail")
                                .description("그룹 챌린지 상세 정보를 조회한다.")
                                .pathParameters(typedPathParameterDescriptors)
                                .responseSchema(schema("GroupChallengeResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
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
                fieldWithPath("participants[].goalTimes[].type").type(JsonFieldType.STRING).description("앱 사용 목표 타입(INSTAGRAM, YOUTUBE, ALL_USE)"),
                fieldWithPath("participants[].goalTimes[].minutes").type(JsonFieldType.NUMBER).description("이번 챌린지에 적용되는 앱 사용 목표 시간(분 단위 정수)"),
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
                fieldWithPath("[].participants[].goalTimes[].type").type(JsonFieldType.STRING).description("앱 사용 목표 타입(INSTAGRAM, YOUTUBE, ALL_USE)"),
                fieldWithPath("[].participants[].goalTimes[].minutes").type(JsonFieldType.NUMBER).description("이번 챌린지에 적용되는 앱 사용 목표 시간(분 단위 정수)"),
                fieldWithPath("[].startAt").type(JsonFieldType.STRING).description("챌린지 시작 일시").optional(),
                fieldWithPath("[].endAt").type(JsonFieldType.STRING).description("챌린지 종료 일시").optional(),
                fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("챌린지 생성 일시"),
                fieldWithPath("[].updatedAt").type(JsonFieldType.STRING).description("챌린지 수정 일시")
        };
    }
}
