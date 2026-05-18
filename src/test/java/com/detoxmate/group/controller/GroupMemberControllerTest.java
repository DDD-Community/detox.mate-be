package com.detoxmate.group.controller;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.group.dto.GroupMemberActivitySummaryResponse;
import com.detoxmate.group.dto.GroupMemberGoalChangeAvailabilityResponse;
import com.detoxmate.group.dto.GroupMemberProfileResponse;
import com.detoxmate.group.dto.GroupMemberUsageGoalResponse;
import com.detoxmate.group.dto.GroupMemberWeeklySummaryResponse;
import com.detoxmate.group.service.GroupMemberProfileService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class GroupMemberControllerTest {

    private GroupMemberProfileService groupMemberProfileService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        groupMemberProfileService = mock(GroupMemberProfileService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://example.com/profile.png", true));

        mockMvc = MockMvcBuilders.standaloneSetup(new GroupMemberController(groupMemberProfileService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 그룹_멤버_프로필을_조회하면_프로필과_목표_통계를_반환한다() throws Exception {
        when(groupMemberProfileService.getGroupMemberProfile(1L, 100L, 1L))
                .thenReturn(groupMemberProfileResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = groupMemberProfilePathParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedPathParameterDescriptors = groupMemberProfileOpenApiPathParameters();
        FieldDescriptor[] responseFieldDescriptors = groupMemberProfileResponseFields();

        mockMvc.perform(get("/groups/{groupId}/members/{groupMemberId}", 1L, 100L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.groupMemberId").value(100))
                .andExpect(jsonPath("$.memberStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.currentGoals[0].id").value(101))
                .andExpect(jsonPath("$.currentGoals[0].createdAt").value("2026-05-01T10:00:00"))
                .andExpect(jsonPath("$.currentGoals[0].setAt").doesNotExist())
                .andExpect(jsonPath("$.activitySummary.achievementRate").value(75))
                .andExpect(jsonPath("$.weeklySummary.averageUsedMinutes").value(90))
                .andDo(document("groups/members/get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group Member")
                                .summary("Get group member profile")
                                .description("같은 그룹의 활성 멤버 프로필, 현재 목표, D-day, 달성 통계를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(typedPathParameterDescriptors)
                                .responseSchema(schema("GroupMemberProfileResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_멤버_프로필_조회_권한이_없으면_403_에러를_반환한다() throws Exception {
        when(groupMemberProfileService.getGroupMemberProfile(1L, 100L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 속한 그룹만 조회할 수 있습니다."));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = groupMemberProfilePathParameters();
        com.epages.restdocs.apispec.ParameterDescriptorWithType[] typedPathParameterDescriptors = groupMemberProfileOpenApiPathParameters();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        mockMvc.perform(get("/groups/{groupId}/members/{groupMemberId}", 1L, 100L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403))
                .andDo(document("groups/members/get-by-id-forbidden",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group Member")
                                .summary("Get group member profile")
                                .description("같은 그룹의 활성 멤버 프로필, 현재 목표, D-day, 달성 통계를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(typedPathParameterDescriptors)
                                .responseSchema(schema("ErrorResponse"))
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    private GroupMemberProfileResponse groupMemberProfileResponse() {
        return new GroupMemberProfileResponse(
                100L,
                1L,
                1L,
                "의진",
                "https://example.com/profile.png",
                "MEMBER",
                "ACTIVE",
                LocalDateTime.of(2026, 5, 1, 23, 50),
                "SET",
                false,
                List.of(
                        new GroupMemberUsageGoalResponse(101L, UsageGoalTypeCode.TOTAL_USAGE, 120, LocalDateTime.of(2026, 5, 1, 10, 0)),
                        new GroupMemberUsageGoalResponse(102L, UsageGoalTypeCode.INSTAGRAM, 30, LocalDateTime.of(2026, 5, 1, 10, 0))
                ),
                new GroupMemberGoalChangeAvailabilityResponse(false, java.time.LocalDate.of(2026, 5, 15), 1),
                new GroupMemberActivitySummaryResponse(java.time.LocalDate.of(2026, 5, 5), 8, 75),
                new GroupMemberWeeklySummaryResponse(java.time.LocalDate.of(2026, 5, 6), java.time.LocalDate.of(2026, 5, 12), 7, 90, 120, 30, 5, 3)
        );
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private ParameterDescriptor[] groupMemberProfilePathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("groupId").description("그룹 ID"),
                parameterWithName("groupMemberId").description("그룹 멤버 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] groupMemberProfileOpenApiPathParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("groupId")
                        .type(SimpleType.INTEGER)
                        .description("그룹 ID"),
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("groupMemberId")
                        .type(SimpleType.INTEGER)
                        .description("그룹 멤버 ID")
        };
    }

    private FieldDescriptor[] groupMemberProfileResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("groupMemberId").type(JsonFieldType.NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("displayName").type(JsonFieldType.STRING).description("사용자 표시 이름"),
                fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("저장된 프로필 이미지 object key를 읽기 URL로 변환한 값").optional(),
                fieldWithPath("role").type(JsonFieldType.STRING).description("그룹 내 역할"),
                fieldWithPath("memberStatus").type(JsonFieldType.STRING).description("그룹 멤버 상태"),
                fieldWithPath("joinedAt").type(JsonFieldType.STRING).description("그룹 참여 일시"),
                fieldWithPath("goalStatus").type(JsonFieldType.STRING).description("목표 설정 상태. SET 또는 NOT_SET"),
                fieldWithPath("isUserWithdrawn").type(JsonFieldType.BOOLEAN).description("회원 탈퇴한 사용자인지 여부"),
                fieldWithPath("currentGoals").type(JsonFieldType.ARRAY).description("목표 타입별 최신 목표 시간"),
                fieldWithPath("currentGoals[].id").type(JsonFieldType.NUMBER).description("user usage goal time ID"),
                fieldWithPath("currentGoals[].usageGoalType").type(JsonFieldType.STRING).description("사용시간 목표 타입"),
                fieldWithPath("currentGoals[].goalMinutes").type(JsonFieldType.NUMBER).description("목표 사용시간(분)"),
                fieldWithPath("currentGoals[].createdAt").type(JsonFieldType.STRING).description("목표 설정 일시"),
                fieldWithPath("goalChangeAvailability").type(JsonFieldType.OBJECT).description("본인 조회 시 목표 변경 가능 상태. 타인 조회이면 null").optional(),
                fieldWithPath("goalChangeAvailability.canChange").type(JsonFieldType.BOOLEAN).description("목표 변경 가능 여부").optional(),
                fieldWithPath("goalChangeAvailability.nextChangeAvailableDate").type(JsonFieldType.STRING).description("다음 목표 변경 가능 날짜").optional(),
                fieldWithPath("goalChangeAvailability.remainingDays").type(JsonFieldType.NUMBER).description("목표 변경까지 남은 날짜 수").optional(),
                fieldWithPath("activitySummary").type(JsonFieldType.OBJECT).description("첫 인증 기준 전체 상태"),
                fieldWithPath("activitySummary.firstCertifiedDate").type(JsonFieldType.STRING).description("첫 인증 날짜. 없으면 null").optional(),
                fieldWithPath("activitySummary.dayCount").type(JsonFieldType.NUMBER).description("첫 인증 기준 D-day 숫자"),
                fieldWithPath("activitySummary.achievementRate").type(JsonFieldType.NUMBER).description("첫 인증 이후 전체 달성률"),
                fieldWithPath("weeklySummary").type(JsonFieldType.OBJECT).description("오늘 포함 최근 7일 집계"),
                fieldWithPath("weeklySummary.startDate").type(JsonFieldType.STRING).description("최근 7일 시작일"),
                fieldWithPath("weeklySummary.endDate").type(JsonFieldType.STRING).description("최근 7일 종료일"),
                fieldWithPath("weeklySummary.totalDays").type(JsonFieldType.NUMBER).description("최근 7일 분모. 항상 7"),
                fieldWithPath("weeklySummary.averageUsedMinutes").type(JsonFieldType.NUMBER).description("미인증일을 0분으로 포함한 평균 사용 시간"),
                fieldWithPath("weeklySummary.goalMinutes").type(JsonFieldType.NUMBER).description("최신 TOTAL_USAGE 목표 시간. 없으면 null").optional(),
                fieldWithPath("weeklySummary.differenceMinutes").type(JsonFieldType.NUMBER).description("목표 시간 - 평균 사용 시간. 목표가 없으면 null").optional(),
                fieldWithPath("weeklySummary.certifiedDays").type(JsonFieldType.NUMBER).description("최근 7일 중 인증한 날짜 수"),
                fieldWithPath("weeklySummary.achievedDays").type(JsonFieldType.NUMBER).description("최근 7일 중 목표 달성 날짜 수")
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
