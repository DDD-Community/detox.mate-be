package com.detoxmate.screentimeocr.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.common.error.GlobalExceptionHandler;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportCreateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportCreateResponse;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.service.ScreenTimeOcrErrorReportService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class ScreenTimeOcrErrorReportControllerTest {

    private ScreenTimeOcrErrorReportService reportService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        reportService = mock(ScreenTimeOcrErrorReportService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://example.com/profile.png"));

        mockMvc = MockMvcBuilders.standaloneSetup(new ScreenTimeOcrErrorReportController(reportService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void screen_time_ocr_error_report를_생성한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = createRequestFields();
        FieldDescriptor[] responseFieldDescriptors = createResponseFields();

        when(reportService.create(1L, new ScreenTimeOcrErrorReportCreateRequest(
                null,
                10L,
                LocalDate.of(2026, 5, 12),
                "screen-time-ocr-reports/1/2026/05/sample.png",
                180
        ))).thenReturn(new ScreenTimeOcrErrorReportCreateResponse(
                555L,
                ScreenTimeOcrErrorReportStatus.PENDING,
                LocalDateTime.of(2026, 5, 12, 21, 31)
        ));

        mockMvc.perform(post("/screen-time-ocr-error-reports")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupChallengeParticipantId": 10,
                                  "recordDate": "2026-05-12",
                                  "imageObjectKey": "screen-time-ocr-reports/1/2026/05/sample.png",
                                  "ocrTotalUsedMinutes": 180
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(555))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-12T21:31:00"))
                .andDo(document("screen-time-ocr-error-reports/create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Screen Time OCR Error Report")
                                .summary("Create screen time OCR error report")
                                .description("스크린타임 OCR 총 사용시간 오류 신고를 생성한다. 활동 인증 기록은 없을 수 있으며 admin 보정 시 사후 연결된다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("ScreenTimeOcrErrorReportCreateRequest"))
                                .responseSchema(schema("ScreenTimeOcrErrorReportCreateResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void report_생성_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/screen-time-ocr-error-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": "2026-05-12",
                                  "imageObjectKey": "screen-time-ocr-reports/1/2026/05/sample.png",
                                  "ocrTotalUsedMinutes": 180
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(reportService);
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] createRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("activityRecordId")
                        .type(JsonFieldType.NUMBER)
                        .optional()
                        .description("이미 생성된 활동 인증 기록 ID. 일반 신고 흐름에서는 없을 수 있으며 admin 보정 시 사후 연결된다."),
                fieldWithPath("groupChallengeParticipantId")
                        .type(JsonFieldType.NUMBER)
                        .description("인증 대상 그룹 챌린지 참여자 ID"),
                fieldWithPath("recordDate")
                        .type(JsonFieldType.STRING)
                        .description("인증 대상 날짜"),
                fieldWithPath("imageObjectKey")
                        .type(JsonFieldType.STRING)
                        .description("스크린타임 OCR 오류 신고 이미지 object key"),
                fieldWithPath("ocrTotalUsedMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("OCR이 추론한 총 사용 시간(분)")
        };
    }

    private FieldDescriptor[] createResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("OCR 오류 신고 ID"),
                fieldWithPath("status")
                        .type(JsonFieldType.STRING)
                        .description("신고 상태"),
                fieldWithPath("createdAt")
                        .type(JsonFieldType.STRING)
                        .description("신고 생성 시각")
        };
    }
}
