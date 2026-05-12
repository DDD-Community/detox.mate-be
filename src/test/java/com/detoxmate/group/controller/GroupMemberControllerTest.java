package com.detoxmate.group.controller;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.group.dto.GroupMemberProfileResponse;
import com.detoxmate.group.dto.GroupMemberUsageGoalResponse;
import com.detoxmate.group.dto.MemberOverallStatsResponse;
import com.detoxmate.group.dto.MemberRecent7DaysStatsResponse;
import com.detoxmate.group.dto.MemberStatsResponse;
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

import java.time.LocalDate;
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
                .thenReturn(new MyProfileResponse(1L, "지민", "https://example.com/profile.png"));

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

        mockMvc.perform(get("/groups/{groupId}/members/{memberId}", 1L, 100L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentGoals[0].id").value(101))
                .andExpect(jsonPath("$.stats.overall.achievementRate").value(43))
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

        mockMvc.perform(get("/groups/{groupId}/members/{memberId}", 1L, 100L)
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
                4,
                false,
                List.of(
                        new GroupMemberUsageGoalResponse(101L, UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 29, 10, 30)),
                        new GroupMemberUsageGoalResponse(102L, UsageGoalTypeCode.INSTAGRAM, 30, LocalDateTime.of(2026, 4, 29, 10, 30))
                ),
                new MemberStatsResponse(
                        new MemberOverallStatsResponse(LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 5), 7, 3, 43),
                        new MemberRecent7DaysStatsResponse(LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 5), 7, 5, 3)
                )
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
                parameterWithName("memberId").description("그룹 멤버 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] groupMemberProfileOpenApiPathParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("groupId")
                        .type(SimpleType.INTEGER)
                        .description("그룹 ID"),
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("memberId")
                        .type(SimpleType.INTEGER)
                        .description("그룹 멤버 ID")
        };
    }

    private FieldDescriptor[] groupMemberProfileResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("displayName").type(JsonFieldType.STRING).description("사용자 표시 이름"),
                fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("저장된 프로필 이미지 object key를 읽기 URL로 변환한 값").optional(),
                fieldWithPath("role").type(JsonFieldType.STRING).description("그룹 내 역할"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("그룹 멤버 상태"),
                fieldWithPath("joinedAt").type(JsonFieldType.STRING).description("그룹 참여 일시"),
                fieldWithPath("dayCount").type(JsonFieldType.NUMBER).description("현재 챌린지 참여일 기준 D-day. 같은 날이면 0"),
                fieldWithPath("isUserWithdrawn").type(JsonFieldType.BOOLEAN).description("회원 탈퇴한 사용자인지 여부"),
                fieldWithPath("currentGoals").type(JsonFieldType.ARRAY).description("목표 타입별 최신 목표 시간"),
                fieldWithPath("currentGoals[].id").type(JsonFieldType.NUMBER).description("user usage goal time ID"),
                fieldWithPath("currentGoals[].usageGoalType").type(JsonFieldType.STRING).description("사용시간 목표 타입"),
                fieldWithPath("currentGoals[].goalMinutes").type(JsonFieldType.NUMBER).description("목표 사용시간(분)"),
                fieldWithPath("currentGoals[].setAt").type(JsonFieldType.STRING).description("목표 설정 일시"),
                fieldWithPath("stats").type(JsonFieldType.OBJECT).description("목표 달성 통계"),
                fieldWithPath("stats.overall").type(JsonFieldType.OBJECT).description("전체 달성률 통계"),
                fieldWithPath("stats.overall.startDate").type(JsonFieldType.STRING).description("전체 달성률 계산 시작일"),
                fieldWithPath("stats.overall.endDate").type(JsonFieldType.STRING).description("전체 달성률 계산 종료일"),
                fieldWithPath("stats.overall.totalDays").type(JsonFieldType.NUMBER).description("전체 달성률 분모"),
                fieldWithPath("stats.overall.achievedDays").type(JsonFieldType.NUMBER).description("전체 달성률 기간 중 달성일 수"),
                fieldWithPath("stats.overall.achievementRate").type(JsonFieldType.NUMBER).description("정수 반올림한 전체 달성률"),
                fieldWithPath("stats.recent7Days").type(JsonFieldType.OBJECT).description("오늘 포함 최근 7일 통계"),
                fieldWithPath("stats.recent7Days.startDate").type(JsonFieldType.STRING).description("최근 7일 시작일"),
                fieldWithPath("stats.recent7Days.endDate").type(JsonFieldType.STRING).description("최근 7일 종료일"),
                fieldWithPath("stats.recent7Days.totalDays").type(JsonFieldType.NUMBER).description("최근 7일 분모. 항상 7"),
                fieldWithPath("stats.recent7Days.submittedDays").type(JsonFieldType.NUMBER).description("최근 7일 중 최종 인증 완료일 수"),
                fieldWithPath("stats.recent7Days.achievedDays").type(JsonFieldType.NUMBER).description("최근 7일 중 목표 달성일 수")
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
