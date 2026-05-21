package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.group.dto.GroupActivityCalendarResponse;
import com.detoxmate.group.dto.GroupActivityCalendarSummaryResponse;
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
                .thenReturn(new MyProfileResponse(1L, "나", "https://example.com/profile/me.png", true));

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
        when(groupActivityCalendarService.getCalendar(10L, 1L))
                .thenReturn(calendarResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = groupChallengeIdPathParameters();
        FieldDescriptor[] responseFieldDescriptors = calendarResponseFields();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/activity-calendar", 10L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstVerificationDate").doesNotExist())
                .andExpect(jsonPath("$.summary.startDate").value("2026-04-10"))
                .andExpect(jsonPath("$.summary.halfCount").value(7))
                .andDo(document("group-challenges/activity-calendar/get",
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
                                .pathParameters(groupChallengeIdOpenApiPathParameters())
                                .responseSchema(schema("GroupActivityCalendarResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    private GroupActivityCalendarResponse calendarResponse() {
        return new GroupActivityCalendarResponse(
                1L,
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

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private ParameterDescriptor[] groupChallengeIdPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("groupChallengeId").description("그룹 챌린지 ID")
        };
    }

    private com.epages.restdocs.apispec.ParameterDescriptorWithType[] groupChallengeIdOpenApiPathParameters() {
        return new com.epages.restdocs.apispec.ParameterDescriptorWithType[] {
                com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName("groupChallengeId")
                        .type(SimpleType.INTEGER)
                        .description("그룹 챌린지 ID")
        };
    }

    private FieldDescriptor[] calendarResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("streakDays").type(JsonFieldType.NUMBER)
                        .description("오늘을 제외하고 어제까지 연속으로 그룹 인증에 성공한 날짜 수"),
                fieldWithPath("summary").type(JsonFieldType.OBJECT).description("첫 인증 시작일 이후 누적 상단 카운트"),
                fieldWithPath("summary.startDate").type(JsonFieldType.STRING)
                        .description("누적 카운트 시작일. 첫 인증 시작일이며, 아직 산정할 수 없으면 null").optional(),
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
}
