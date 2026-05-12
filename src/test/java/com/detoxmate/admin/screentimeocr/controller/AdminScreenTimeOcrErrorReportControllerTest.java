package com.detoxmate.admin.screentimeocr.controller;

import com.detoxmate.admin.service.AdminAuthorizationService;
import com.detoxmate.common.error.GlobalExceptionHandler;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.dto.AdminScreenTimeOcrErrorReportItemResponse;
import com.detoxmate.screentimeocr.dto.AdminScreenTimeOcrErrorReportListResponse;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateAction;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateResponse;
import com.detoxmate.screentimeocr.service.ScreenTimeOcrErrorReportAdminService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class AdminScreenTimeOcrErrorReportControllerTest {

    private ScreenTimeOcrErrorReportAdminService adminReportService;
    private AdminAuthorizationService adminAuthorizationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        adminReportService = mock(ScreenTimeOcrErrorReportAdminService.class);
        adminAuthorizationService = mock(AdminAuthorizationService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminScreenTimeOcrErrorReportController(
                        adminReportService,
                        adminAuthorizationService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void admin은_screen_time_ocr_error_report_목록을_조회한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] queryParameterDescriptors = listQueryParameters();
        FieldDescriptor[] responseFieldDescriptors = listResponseFields();

        when(adminReportService.list(ScreenTimeOcrErrorReportStatus.PENDING, PageRequest.of(0, 20)))
                .thenReturn(new AdminScreenTimeOcrErrorReportListResponse(
                        List.of(new AdminScreenTimeOcrErrorReportItemResponse(
                                555L,
                                1L,
                                "지민",
                                123L,
                                10L,
                                LocalDate.of(2026, 5, 12),
                                "https://example.com/media/screen-time-ocr-reports/1/2026/05/sample.png",
                                180,
                                null,
                                ScreenTimeOcrErrorReportStatus.PENDING,
                                null,
                                null,
                                LocalDateTime.of(2026, 5, 12, 21, 31),
                                LocalDateTime.of(2026, 5, 12, 21, 31)
                        )),
                        0,
                        20,
                        1L,
                        1
                ));

        mockMvc.perform(get("/admin/screen-time-ocr-error-reports")
                        .header("X-Admin-Token", "test-admin-token")
                        .queryParam("status", "PENDING")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].id").value(555))
                .andExpect(jsonPath("$.items[0].imageUrl").value("https://example.com/media/screen-time-ocr-reports/1/2026/05/sample.png"))
                .andExpect(jsonPath("$.items[0].ocrTotalUsedMinutes").value(180))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andDo(document("admin/screen-time-ocr-error-reports/list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        queryParameters(queryParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin Screen Time OCR Error Report")
                                .summary("List screen time OCR error reports")
                                .description("admin이 스크린타임 OCR 오류 신고 목록을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .queryParameters(queryParameterDescriptors)
                                .responseSchema(schema("AdminScreenTimeOcrErrorReportListResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void admin은_screen_time_ocr_error_report를_수정한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = reportIdPathParameter();
        FieldDescriptor[] requestFieldDescriptors = updateRequestFields();
        FieldDescriptor[] responseFieldDescriptors = updateResponseFields();

        when(adminReportService.update(555L, new ScreenTimeOcrErrorReportUpdateRequest(
                ScreenTimeOcrErrorReportUpdateAction.CORRECT,
                165,
                "스크린샷 기준 총 사용시간 2시간 45분"
        ))).thenReturn(new ScreenTimeOcrErrorReportUpdateResponse(
                555L,
                ScreenTimeOcrErrorReportStatus.CORRECTED,
                180,
                165,
                "스크린샷 기준 총 사용시간 2시간 45분",
                LocalDateTime.of(2026, 5, 13, 10, 0)
        ));

        mockMvc.perform(patch("/admin/screen-time-ocr-error-reports/{reportId}", 555L)
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "CORRECT",
                                  "correctedTotalUsedMinutes": 165,
                                  "adminNote": "스크린샷 기준 총 사용시간 2시간 45분"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(555))
                .andExpect(jsonPath("$.status").value("CORRECTED"))
                .andExpect(jsonPath("$.correctedTotalUsedMinutes").value(165))
                .andDo(document("admin/screen-time-ocr-error-reports/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin Screen Time OCR Error Report")
                                .summary("Update screen time OCR error report")
                                .description("admin이 스크린타임 OCR 오류 신고를 수정 또는 반려한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .requestSchema(schema("ScreenTimeOcrErrorReportUpdateRequest"))
                                .responseSchema(schema("ScreenTimeOcrErrorReportUpdateResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("X-Admin-Token").description("서버에 설정된 admin 검수 토큰")
        };
    }

    private ParameterDescriptor[] listQueryParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("status").optional().description("신고 상태 필터. 기본값 `PENDING`"),
                parameterWithName("page").optional().description("0-base 페이지 번호. 기본값 `0`"),
                parameterWithName("size").optional().description("페이지 크기. 기본값 `20`")
        };
    }

    private ParameterDescriptor[] reportIdPathParameter() {
        return new ParameterDescriptor[] {
                parameterWithName("reportId").description("OCR 오류 신고 ID")
        };
    }

    private FieldDescriptor[] listResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("items[]").type(JsonFieldType.ARRAY).description("OCR 오류 신고 목록"),
                fieldWithPath("items[].id").type(JsonFieldType.NUMBER).description("OCR 오류 신고 ID"),
                fieldWithPath("items[].userId").type(JsonFieldType.NUMBER).description("신고한 유저 ID"),
                fieldWithPath("items[].userDisplayName").type(JsonFieldType.STRING).description("신고한 유저 표시 이름"),
                fieldWithPath("items[].activityRecordId").type(JsonFieldType.NUMBER).optional().description("연결된 활동 인증 기록 ID"),
                fieldWithPath("items[].groupChallengeParticipantId").type(JsonFieldType.NUMBER).description("연결된 그룹 챌린지 참여자 ID"),
                fieldWithPath("items[].recordDate").type(JsonFieldType.STRING).description("인증 대상 날짜"),
                fieldWithPath("items[].imageUrl").type(JsonFieldType.STRING).description("검수용 스크린타임 이미지 URL"),
                fieldWithPath("items[].ocrTotalUsedMinutes").type(JsonFieldType.NUMBER).description("OCR이 추론한 총 사용 시간(분)"),
                fieldWithPath("items[].correctedTotalUsedMinutes").type(JsonFieldType.NUMBER).optional().description("admin이 수정한 총 사용 시간(분)"),
                fieldWithPath("items[].status").type(JsonFieldType.STRING).description("신고 상태"),
                fieldWithPath("items[].adminNote").type(JsonFieldType.STRING).optional().description("admin 처리 메모"),
                fieldWithPath("items[].resolvedAt").type(JsonFieldType.STRING).optional().description("처리 시각"),
                fieldWithPath("items[].createdAt").type(JsonFieldType.STRING).description("생성 시각"),
                fieldWithPath("items[].updatedAt").type(JsonFieldType.STRING).description("수정 시각"),
                fieldWithPath("page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
        };
    }

    private FieldDescriptor[] updateRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("action").type(JsonFieldType.STRING).description("처리 동작. `CORRECT` 또는 `REJECT`"),
                fieldWithPath("correctedTotalUsedMinutes").type(JsonFieldType.NUMBER).optional().description("수정된 총 사용 시간. `CORRECT`일 때 필수"),
                fieldWithPath("adminNote").type(JsonFieldType.STRING).optional().description("admin 처리 메모")
        };
    }

    private FieldDescriptor[] updateResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("OCR 오류 신고 ID"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("변경된 신고 상태"),
                fieldWithPath("ocrTotalUsedMinutes").type(JsonFieldType.NUMBER).description("OCR이 추론한 총 사용 시간(분)"),
                fieldWithPath("correctedTotalUsedMinutes").type(JsonFieldType.NUMBER).optional().description("admin이 수정한 총 사용 시간(분)"),
                fieldWithPath("adminNote").type(JsonFieldType.STRING).optional().description("admin 처리 메모"),
                fieldWithPath("resolvedAt").type(JsonFieldType.STRING).description("처리 시각")
        };
    }
}
