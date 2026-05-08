package com.detoxmate.group.controller;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.group.dto.ActivityRecordDetailHistoryResponse;
import com.detoxmate.group.dto.ActivityRecordHistoryResponse;
import com.detoxmate.group.dto.CalendarHistoryMemberResponse;
import com.detoxmate.group.dto.GroupActivityCalendarHistoryResponse;
import com.detoxmate.group.dto.GroupActivityCalendarResponse;
import com.detoxmate.group.dto.GroupActivityCalendarSummaryResponse;
import com.detoxmate.group.dto.GroupDailyVerificationSummaryResponse;
import com.detoxmate.group.dto.MemberDailyGoalResponse;
import com.detoxmate.group.service.GroupActivityCalendarService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
import org.junit.jupiter.api.BeforeEach;
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
class GroupActivityCalendarControllerTest {

    private GroupActivityCalendarService groupActivityCalendarService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        groupActivityCalendarService = mock(GroupActivityCalendarService.class);
        userService = mock(UserService.class);

        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "나", "https://example.com/profile/me.png"));

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new GroupActivityCalendarController(groupActivityCalendarService)
                )
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 그룹_활동_캘린더_요약을_조회한다() throws Exception {
        when(groupActivityCalendarService.getCalendar(1L, 1L))
                .thenReturn(calendarResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = groupIdPathParameters();
        FieldDescriptor[] responseFieldDescriptors = calendarResponseFields();

        mockMvc.perform(get("/groups/{groupId}/activity-calendar", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstVerificationDate").value("2026-04-10"))
                .andExpect(jsonPath("$.summary.halfCount").value(7))
                .andDo(document("groups/activity-calendar/get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group Activity Calendar")
                                .summary("Get group activity calendar summary")
                                .description("첫 인증 시작일 이후의 그룹 인증 누적 요약과 오늘을 제외한 그룹 스트릭을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(groupIdOpenApiPathParameters())
                                .responseSchema(schema("GroupActivityCalendarResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 그룹_활동_캘린더_히스토리를_조회한다() throws Exception {
        when(groupActivityCalendarService.getCalendarHistory(1L, LocalDate.of(2026, 4, 10), 1L))
                .thenReturn(calendarHistoryResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = historyPathParameters();
        FieldDescriptor[] responseFieldDescriptors = calendarHistoryResponseFields();

        mockMvc.perform(get("/groups/{groupId}/activity-calendar/days/{date}", 1L, "2026-04-10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dailySummary.result").value("HALF"))
                .andExpect(jsonPath("$.members[0].dailyStatus").value("GOAL_ACHIEVED"))
                .andDo(document("groups/activity-calendar/history-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Group Activity Calendar")
                                .summary("Get group activity calendar daily history")
                                .description("캘린더에서 선택한 날짜의 멤버별 상태, 유효 목표, 활동 인증 내용을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(historyOpenApiPathParameters())
                                .responseSchema(schema("GroupActivityCalendarHistoryResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    private GroupActivityCalendarResponse calendarResponse() {
        return new GroupActivityCalendarResponse(
                1L,
                LocalDate.of(2026, 4, 10),
                4,
                new GroupActivityCalendarSummaryResponse(
                        LocalDate.of(2026, 4, 10),
                        LocalDate.of(2026, 5, 7),
                        0,
                        7,
                        3
                )
        );
    }

    private GroupActivityCalendarHistoryResponse calendarHistoryResponse() {
        return new GroupActivityCalendarHistoryResponse(
                1L,
                LocalDate.of(2026, 4, 10),
                new GroupDailyVerificationSummaryResponse(
                        LocalDate.of(2026, 4, 10),
                        "CONFIRMED",
                        "HALF",
                        4,
                        2,
                        2
                ),
                List.of(
                        new CalendarHistoryMemberResponse(
                                100L,
                                1000L,
                                2L,
                                "지수",
                                "https://example.com/profile/jisu.png",
                                false,
                                "ACTIVE",
                                "JOINED",
                                "GOAL_ACHIEVED",
                                true,
                                List.of(new MemberDailyGoalResponse(
                                        101L,
                                        UsageGoalTypeCode.TOTAL_USAGE,
                                        90,
                                        LocalDate.of(2026, 4, 10)
                                )),
                                new ActivityRecordHistoryResponse(
                                        123L,
                                        LocalDateTime.of(2026, 4, 10, 21, 30),
                                        "https://example.com/activity-records/1/2026/04/run.png",
                                        "2시간동안 러닝 뛰고 온 날!",
                                        true,
                                        List.of(new ActivityRecordDetailHistoryResponse(
                                                UsageGoalTypeCode.TOTAL_USAGE,
                                                70,
                                                90,
                                                true
                                        )),
                                        22,
                                        10
                                )
                        ),
                        new CalendarHistoryMemberResponse(
                                102L,
                                1002L,
                                1L,
                                "나",
                                "https://example.com/profile/me.png",
                                true,
                                "ACTIVE",
                                "JOINED",
                                "NOT_CERTIFIED",
                                true,
                                List.of(new MemberDailyGoalResponse(
                                        301L,
                                        UsageGoalTypeCode.TOTAL_USAGE,
                                        90,
                                        LocalDate.of(2026, 4, 10)
                                )),
                                null
                        ),
                        new CalendarHistoryMemberResponse(
                                103L,
                                1003L,
                                3L,
                                "서연",
                                "https://example.com/profile/seoyeon.png",
                                false,
                                "LEFT",
                                "WITHDRAWN",
                                "NOT_ACTIVE",
                                false,
                                List.of(new MemberDailyGoalResponse(
                                        401L,
                                        UsageGoalTypeCode.TOTAL_USAGE,
                                        70,
                                        LocalDate.of(2026, 4, 10)
                                )),
                                null
                        )
                )
        );
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private ParameterDescriptor[] groupIdPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("groupId").description("그룹 ID")
        };
    }

    private ParameterDescriptor[] historyPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("groupId").description("그룹 ID"),
                parameterWithName("date").description("조회 날짜. YYYY-MM-DD 형식")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] groupIdOpenApiPathParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("groupId")
                        .type(SimpleType.INTEGER)
                        .description("그룹 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] historyOpenApiPathParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("groupId")
                        .type(SimpleType.INTEGER)
                        .description("그룹 ID"),
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("date")
                        .type(SimpleType.STRING)
                        .description("조회 날짜. YYYY-MM-DD 형식")
        };
    }

    private FieldDescriptor[] calendarResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("firstVerificationDate").type(JsonFieldType.STRING)
                        .description("첫 인증 시작일. 아직 산정할 수 없으면 null").optional(),
                fieldWithPath("streakDays").type(JsonFieldType.NUMBER)
                        .description("오늘을 제외하고 어제까지 연속으로 그룹 인증에 성공한 날짜 수"),
                fieldWithPath("summary").type(JsonFieldType.OBJECT).description("첫 인증 시작일 이후 누적 상단 카운트"),
                fieldWithPath("summary.startDate").type(JsonFieldType.STRING)
                        .description("누적 카운트 시작일. firstVerificationDate와 같다").optional(),
                fieldWithPath("summary.endDate").type(JsonFieldType.STRING)
                        .description("누적 카운트 종료일. 보통 조회일 기준 어제").optional(),
                fieldWithPath("summary.allCount").type(JsonFieldType.NUMBER)
                        .description("startDate부터 endDate까지 ALL인 날짜 수"),
                fieldWithPath("summary.halfCount").type(JsonFieldType.NUMBER)
                        .description("startDate부터 endDate까지 HALF인 날짜 수"),
                fieldWithPath("summary.resetCount").type(JsonFieldType.NUMBER)
                        .description("startDate부터 endDate까지 RESET인 날짜 수")
        };
    }

    private FieldDescriptor[] calendarHistoryResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("date").type(JsonFieldType.STRING).description("조회 날짜"),
                fieldWithPath("dailySummary").type(JsonFieldType.OBJECT).description("선택 날짜의 그룹 인증 집계"),
                fieldWithPath("dailySummary.date").type(JsonFieldType.STRING).description("KST 기준 날짜"),
                fieldWithPath("dailySummary.dayStatus").type(JsonFieldType.STRING)
                        .description("날짜 확정 상태 (NOT_STARTED | CONFIRMED | IN_PROGRESS | FUTURE)"),
                fieldWithPath("dailySummary.result").type(JsonFieldType.STRING)
                        .description("일별 그룹 인증 결과 (ALL | HALF | RESET). 확정 전이면 null").optional(),
                fieldWithPath("dailySummary.activeMemberCount").type(JsonFieldType.NUMBER)
                        .description("해당 날짜의 활동중 멤버 수"),
                fieldWithPath("dailySummary.certifiedMemberCount").type(JsonFieldType.NUMBER)
                        .description("해당 날짜에 인증한 활동중 멤버 수"),
                fieldWithPath("dailySummary.requiredCount").type(JsonFieldType.NUMBER)
                        .description("그룹 인증 성공에 필요한 인증자 수"),
                fieldWithPath("members").type(JsonFieldType.ARRAY).description("선택 날짜의 멤버별 활동 인증 히스토리"),
                fieldWithPath("members[].groupMemberId").type(JsonFieldType.NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("members[].groupChallengeParticipantId").type(JsonFieldType.NUMBER)
                        .description("그룹 챌린지 참가자 ID"),
                fieldWithPath("members[].userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("members[].displayName").type(JsonFieldType.STRING).description("사용자 표시 이름"),
                fieldWithPath("members[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("members[].isMe").type(JsonFieldType.BOOLEAN).description("로그인 사용자인지 여부"),
                fieldWithPath("members[].memberStatus").type(JsonFieldType.STRING).description("그룹 멤버 상태"),
                fieldWithPath("members[].participantStatus").type(JsonFieldType.STRING).description("챌린지 참가 상태"),
                fieldWithPath("members[].dailyStatus").type(JsonFieldType.STRING)
                        .description("멤버 일별 상태 (GOAL_ACHIEVED | GOAL_FAILED | NOT_CERTIFIED | NOT_ACTIVE)"),
                fieldWithPath("members[].includedInGroupResult").type(JsonFieldType.BOOLEAN)
                        .description("해당 날짜 그룹 인증 분모/분자 계산 대상인지 여부"),
                fieldWithPath("members[].goals").type(JsonFieldType.ARRAY).description("선택 날짜에 유효한 목표 목록"),
                fieldWithPath("members[].goals[].userUsageGoalTimeId").type(JsonFieldType.NUMBER)
                        .description("user_usage_goal_times ID"),
                fieldWithPath("members[].goals[].usageGoalType").type(JsonFieldType.STRING).description("목표 타입"),
                fieldWithPath("members[].goals[].goalMinutes").type(JsonFieldType.NUMBER).description("목표 시간(분)"),
                fieldWithPath("members[].goals[].effectiveDate").type(JsonFieldType.STRING).description("목표 적용 시작일"),
                fieldWithPath("members[].activityRecord").type(JsonFieldType.VARIES)
                        .description("선택 날짜의 최종 활동 인증 내용. 인증하지 않았으면 null").optional(),
                fieldWithPath("members[].activityRecord.id").type(JsonFieldType.NUMBER).description("활동 인증 기록 ID").optional(),
                fieldWithPath("members[].activityRecord.submittedAt").type(JsonFieldType.STRING)
                        .description("최종 인증 제출 일시").optional(),
                fieldWithPath("members[].activityRecord.activityImageUrl").type(JsonFieldType.STRING)
                        .description("활동 이미지 URL").optional(),
                fieldWithPath("members[].activityRecord.reflectionText").type(JsonFieldType.STRING)
                        .description("회고 텍스트").optional(),
                fieldWithPath("members[].activityRecord.allAchieved").type(JsonFieldType.BOOLEAN)
                        .description("인증의 모든 detail이 목표를 달성했는지 여부").optional(),
                fieldWithPath("members[].activityRecord.details").type(JsonFieldType.ARRAY)
                        .description("목표 타입별 사용 시간과 달성 여부").optional(),
                fieldWithPath("members[].activityRecord.details[].usageGoalType").type(JsonFieldType.STRING)
                        .description("목표 타입").optional(),
                fieldWithPath("members[].activityRecord.details[].usedMinutes").type(JsonFieldType.NUMBER)
                        .description("인증 시 제출된 사용 시간(분)").optional(),
                fieldWithPath("members[].activityRecord.details[].goalMinutes").type(JsonFieldType.NUMBER)
                        .description("인증 날짜에 유효했던 목표 시간(분)").optional(),
                fieldWithPath("members[].activityRecord.details[].isAchieved").type(JsonFieldType.BOOLEAN)
                        .description("해당 목표 타입의 달성 여부").optional(),
                fieldWithPath("members[].activityRecord.reactionCount").type(JsonFieldType.NUMBER)
                        .description("해당 인증에 달린 반응 수").optional(),
                fieldWithPath("members[].activityRecord.commentCount").type(JsonFieldType.NUMBER)
                        .description("해당 인증에 달린 댓글 수").optional()
        };
    }
}
