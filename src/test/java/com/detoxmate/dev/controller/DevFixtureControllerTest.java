package com.detoxmate.dev.controller;

import com.detoxmate.dev.dto.ActivityCalendarRichFixtureResponse;
import com.detoxmate.dev.dto.FixtureCheckDatesResponse;
import com.detoxmate.dev.dto.FixtureSummaryResponse;
import com.detoxmate.dev.dto.FixtureUserResponse;
import com.detoxmate.dev.service.ActivityCalendarRichFixtureService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class DevFixtureControllerTest {

    private ActivityCalendarRichFixtureService fixtureService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        fixtureService = mock(ActivityCalendarRichFixtureService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DevFixtureController(fixtureService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void activity_calendar_rich_fixture를_생성한다() throws Exception {
        LocalDate today = LocalDate.of(2026, 5, 9);
        when(fixtureService.seed())
                .thenReturn(new ActivityCalendarRichFixtureResponse(
                        "activity-calendar-rich",
                        1L,
                        10L,
                        "ACR01",
                        today,
                        today.minusDays(8),
                        new FixtureSummaryResponse(4, 4, 0, 8),
                        new FixtureCheckDatesResponse(today.minusDays(8), today.minusDays(7), today),
                        List.of(
                                new FixtureUserResponse("me", 1L, "캘린더 나", "access-token-me"),
                                new FixtureUserResponse("member", 2L, "캘린더 지수", "access-token-jisu"),
                                new FixtureUserResponse("member", 3L, "캘린더 민준", "access-token-minjun")
                        )
                ));
        FieldDescriptor[] responseFieldDescriptors = activityCalendarRichFixtureResponseFields();

        mockMvc.perform(post("/dev/fixtures/activity-calendar-rich"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fixture").value("activity-calendar-rich"))
                .andExpect(jsonPath("$.inviteCode").value("ACR01"))
                .andExpect(jsonPath("$.summary.streakDays").value(8))
                .andDo(result -> verify(fixtureService).seed())
                .andDo(document("dev-fixtures/activity-calendar-rich",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Dev Fixture")
                                .summary("Seed activity calendar rich fixture")
                                .description("local/dev 환경에서 그룹 활동 캘린더 happy case 검증용 fixture를 삭제 후 재생성한다.")
                                .responseSchema(schema("ActivityCalendarRichFixtureResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    private FieldDescriptor[] activityCalendarRichFixtureResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("fixture").type(JsonFieldType.STRING).description("fixture 이름"),
                fieldWithPath("groupId").type(JsonFieldType.NUMBER).description("생성된 그룹 ID"),
                fieldWithPath("groupChallengeId").type(JsonFieldType.NUMBER).description("생성된 그룹 챌린지 ID"),
                fieldWithPath("inviteCode").type(JsonFieldType.STRING).description("고정 초대코드"),
                fieldWithPath("today").type(JsonFieldType.STRING).description("fixture 생성 시점의 KST 오늘 날짜"),
                fieldWithPath("firstVerificationDate").type(JsonFieldType.STRING).description("첫 그룹 인증 평가일"),
                fieldWithPath("summary.allCount").type(JsonFieldType.NUMBER).description("첫 인증 시작일 이후 ALL 날짜 수"),
                fieldWithPath("summary.halfCount").type(JsonFieldType.NUMBER).description("첫 인증 시작일 이후 HALF 날짜 수"),
                fieldWithPath("summary.resetCount").type(JsonFieldType.NUMBER).description("첫 인증 시작일 이후 RESET 날짜 수"),
                fieldWithPath("summary.streakDays").type(JsonFieldType.NUMBER).description("어제까지의 그룹 스트릭"),
                fieldWithPath("checkDates.allDay").type(JsonFieldType.STRING).description("ALL 결과 확인용 날짜"),
                fieldWithPath("checkDates.halfDay").type(JsonFieldType.STRING).description("HALF 결과 확인용 날짜"),
                fieldWithPath("checkDates.today").type(JsonFieldType.STRING).description("IN_PROGRESS 확인용 오늘 날짜"),
                fieldWithPath("users").type(JsonFieldType.ARRAY).description("fixture 테스트 유저 목록"),
                fieldWithPath("users[].role").type(JsonFieldType.STRING).description("fixture 내 역할"),
                fieldWithPath("users[].userId").type(JsonFieldType.NUMBER).description("유저 ID"),
                fieldWithPath("users[].displayName").type(JsonFieldType.STRING).description("유저 표시 이름"),
                fieldWithPath("users[].accessToken").type(JsonFieldType.STRING).description("로컬 API 호출용 access token")
        };
    }
}
