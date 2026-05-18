package com.detoxmate.activityrecord.controller;

import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailResult;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.service.ActivityRecordService;
import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.common.error.GlobalExceptionHandler;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

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
class ActivityRecordControllerTest {

    private static final String TEST_IMAGE_BASE_URL = "https://example.com/media";

    private ActivityRecordService activityRecordService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        activityRecordService = mock(ActivityRecordService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://example.com/profile.png", true));

        mockMvc = MockMvcBuilders.standaloneSetup(new ActivityRecordController(activityRecordService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 활동기록_달성여부를_확인하면_detail별_결과를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = achievementCheckRequestFields();
        FieldDescriptor[] responseFieldDescriptors = achievementCheckResponseFields();

        when(activityRecordService.checkAchievement(1L, new ActivityRecordAchievementCheckRequest(List.of(
                new ActivityRecordDetailRequest(UsageGoalTypeCode.TOTAL_USAGE, 80),
                new ActivityRecordDetailRequest(UsageGoalTypeCode.INSTAGRAM, 20)
        )))).thenReturn(new ActivityRecordAchievementCheckResponse(
                List.of(
                        new ActivityRecordDetailResult(UsageGoalTypeCode.TOTAL_USAGE, 80, 60, false),
                        new ActivityRecordDetailResult(UsageGoalTypeCode.INSTAGRAM, 20, 30, true)
                ),
                false
        ));

        mockMvc.perform(post("/activity-records/achievement-check")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "details": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "usedMinutes": 80
                                    },
                                    {
                                      "usageGoalType": "INSTAGRAM",
                                      "usedMinutes": 20
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.details[0].usageGoalType").value("TOTAL_USAGE"))
                .andExpect(jsonPath("$.details[0].goalMinutes").value(60))
                .andExpect(jsonPath("$.details[0].isAchieved").value(false))
                .andExpect(jsonPath("$.details[1].usageGoalType").value("INSTAGRAM"))
                .andExpect(jsonPath("$.details[1].goalMinutes").value(30))
                .andExpect(jsonPath("$.details[1].isAchieved").value(true))
                .andExpect(jsonPath("$.allAchieved").value(false))
                .andDo(document("activity-records/achievement-check",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Activity Record")
                                .summary("Check activity record achievement")
                                .description("목표 타입별 사용시간을 받아 detail별 달성 여부를 계산한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("ActivityRecordAchievementCheckRequest"))
                                .responseSchema(schema("ActivityRecordAchievementCheckResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 활동기록_달성여부_확인_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/activity-records/achievement-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "details": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "usedMinutes": 80
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(activityRecordService);
    }

    @Test
    void activity_record를_생성하면_생성결과를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = createRequestFields();
        FieldDescriptor[] responseFieldDescriptors = createResponseFields();

        when(activityRecordService.create(1L, new ActivityRecordCreateRequest(
                "activity-records/1/2026/04/sample.png",
                "오늘은 산책했다",
                10L,
                List.of(
                        new ActivityRecordDetailRequest(UsageGoalTypeCode.TOTAL_USAGE, 80),
                        new ActivityRecordDetailRequest(UsageGoalTypeCode.INSTAGRAM, 20)
                )
        ))).thenReturn(new ActivityRecordCreateResponse(
                123L,
                LocalDateTime.of(2026, 4, 26, 21, 30),
                10L,
                TEST_IMAGE_BASE_URL + "/activity-records/1/2026/04/sample.png",
                "오늘은 산책했다",
                List.of(
                        new ActivityRecordDetailResult(UsageGoalTypeCode.TOTAL_USAGE, 80, 60, false),
                        new ActivityRecordDetailResult(UsageGoalTypeCode.INSTAGRAM, 20, 30, true)
                ),
                false
        ));

        mockMvc.perform(post("/activity-records")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityImageObjectKey": "activity-records/1/2026/04/sample.png",
                                  "reflectionText": "오늘은 산책했다",
                                  "groupChallengeParticipantId": 10,
                                  "details": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "usedMinutes": 80
                                    },
                                    {
                                      "usageGoalType": "INSTAGRAM",
                                      "usedMinutes": 20
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.createdAt").value("2026-04-26T21:30:00"))
                .andExpect(jsonPath("$.groupChallengeParticipantId").value(10))
                .andExpect(jsonPath("$.activityImageUrl").value(TEST_IMAGE_BASE_URL + "/activity-records/1/2026/04/sample.png"))
                .andExpect(jsonPath("$.reflectionText").value("오늘은 산책했다"))
                .andExpect(jsonPath("$.details[0].usageGoalType").value("TOTAL_USAGE"))
                .andExpect(jsonPath("$.details[0].isAchieved").value(false))
                .andExpect(jsonPath("$.details[1].usageGoalType").value("INSTAGRAM"))
                .andExpect(jsonPath("$.details[1].isAchieved").value(true))
                .andExpect(jsonPath("$.allAchieved").value(false))
                .andDo(document("activity-records/create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Activity Record")
                                .summary("Create activity record")
                                .description("회고와 목표 타입별 사용시간을 받아 activity record를 생성한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("ActivityRecordCreateRequest"))
                                .responseSchema(schema("ActivityRecordCreateResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void activity_record_생성_중_400_에러를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = createRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        when(activityRecordService.create(1L, new ActivityRecordCreateRequest(
                "activity-records/1/2026/04/sample.png",
                "오늘은 산책했다",
                10L,
                List.of(
                        new ActivityRecordDetailRequest(UsageGoalTypeCode.TOTAL_USAGE, 80),
                        new ActivityRecordDetailRequest(UsageGoalTypeCode.INSTAGRAM, 20)
                )
        ))).thenThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "활동 기록을 생성할 수 없습니다."
        ));

        mockMvc.perform(post("/activity-records")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityImageObjectKey": "activity-records/1/2026/04/sample.png",
                                  "reflectionText": "오늘은 산책했다",
                                  "groupChallengeParticipantId": 10,
                                  "details": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "usedMinutes": 80
                                    },
                                    {
                                      "usageGoalType": "INSTAGRAM",
                                      "usedMinutes": 20
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("activity-records/create-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Activity Record")
                                .summary("Create activity record")
                                .description("회고와 목표 타입별 사용시간을 받아 activity record를 생성한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("ActivityRecordCreateRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void activity_record_생성_요청에_groupChallengeParticipantId가_없으면_400_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/activity-records")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityImageObjectKey": "activity-records/1/2026/04/sample.png",
                                  "reflectionText": "오늘은 산책했다",
                                  "details": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "usedMinutes": 80
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(activityRecordService);
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] achievementCheckRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("details")
                        .type(JsonFieldType.ARRAY)
                        .description("목표 타입별 사용시간 배열"),
                fieldWithPath("details[].usageGoalType")
                        .type(JsonFieldType.STRING)
                        .description("사용시간 목표 타입 (`TOTAL_USAGE`, `INSTAGRAM`, `YOUTUBE`)"),
                fieldWithPath("details[].usedMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("OCR 로 추출한 사용시간(분)")
        };
    }

    private FieldDescriptor[] achievementCheckResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("details")
                        .type(JsonFieldType.ARRAY)
                        .description("목표 타입별 달성 여부 결과"),
                fieldWithPath("details[].usageGoalType")
                        .type(JsonFieldType.STRING)
                        .description("사용시간 목표 타입"),
                fieldWithPath("details[].usedMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("실제 사용시간(분)"),
                fieldWithPath("details[].goalMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("현재 목표 사용시간(분)"),
                fieldWithPath("details[].isAchieved")
                        .type(JsonFieldType.BOOLEAN)
                        .description("해당 목표 타입 달성 여부"),
                fieldWithPath("allAchieved")
                        .type(JsonFieldType.BOOLEAN)
                        .description("하나라도 미달성인 detail 이 있는지 여부")
        };
    }

    private FieldDescriptor[] createRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("activityImageObjectKey")
                        .type(JsonFieldType.STRING)
                        .description("S3 에 업로드한 활동 이미지 object key")
                        .optional(),
                fieldWithPath("reflectionText")
                        .type(JsonFieldType.STRING)
                        .description("회고 텍스트")
                        .optional(),
                fieldWithPath("groupChallengeParticipantId")
                        .type(JsonFieldType.NUMBER)
                        .description("활동 기록을 남길 그룹 챌린지 참여 ID"),
                fieldWithPath("details")
                        .type(JsonFieldType.ARRAY)
                        .description("목표 타입별 사용시간 배열"),
                fieldWithPath("details[].usageGoalType")
                        .type(JsonFieldType.STRING)
                        .description("사용시간 목표 타입 (`TOTAL_USAGE`, `INSTAGRAM`, `YOUTUBE`)"),
                fieldWithPath("details[].usedMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("OCR 로 추출한 사용시간(분)")
        };
    }

    private FieldDescriptor[] createResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("생성된 activity record ID"),
                fieldWithPath("createdAt")
                        .type(JsonFieldType.STRING)
                        .description("생성 시각"),
                fieldWithPath("groupChallengeParticipantId")
                        .type(JsonFieldType.NUMBER)
                        .description("활동 기록이 속한 그룹 챌린지 참여 ID"),
                fieldWithPath("activityImageUrl")
                        .type(JsonFieldType.STRING)
                        .description("활동 이미지 접근 URL. 초기에는 public S3 base URL과 object key를 조합한 값")
                        .optional(),
                fieldWithPath("reflectionText")
                        .type(JsonFieldType.STRING)
                        .description("저장된 회고 텍스트")
                        .optional(),
                fieldWithPath("details")
                        .type(JsonFieldType.ARRAY)
                        .description("목표 타입별 저장 결과"),
                fieldWithPath("details[].usageGoalType")
                        .type(JsonFieldType.STRING)
                        .description("사용시간 목표 타입"),
                fieldWithPath("details[].usedMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("실제 사용시간(분)"),
                fieldWithPath("details[].goalMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("현재 목표 사용시간(분)"),
                fieldWithPath("details[].isAchieved")
                        .type(JsonFieldType.BOOLEAN)
                        .description("해당 목표 타입 달성 여부"),
                fieldWithPath("allAchieved")
                        .type(JsonFieldType.BOOLEAN)
                        .description("하나라도 미달성인 detail 이 있는지 여부")
        };
    }

    private FieldDescriptor[] errorResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("code")
                        .type(JsonFieldType.STRING)
                        .description("에러 코드"),
                fieldWithPath("message")
                        .type(JsonFieldType.STRING)
                        .description("에러 메시지"),
                fieldWithPath("status")
                        .type(JsonFieldType.NUMBER)
                        .description("HTTP 상태 코드")
        };
    }
}
