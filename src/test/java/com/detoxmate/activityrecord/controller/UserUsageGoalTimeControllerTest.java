package com.detoxmate.activityrecord.controller;

import com.detoxmate.activityrecord.dto.CurrentUsageGoalTimeResponse;
import com.detoxmate.activityrecord.dto.CurrentUsageGoalTimesResponse;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.dto.UsageGoalTimeResponse;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimeRequest;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimesSetRequest;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimesSetResponse;
import com.detoxmate.activityrecord.service.UserUsageGoalTimeService;
import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.common.error.GlobalExceptionHandler;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class UserUsageGoalTimeControllerTest {

    private UserUsageGoalTimeService userUsageGoalTimeService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        userUsageGoalTimeService = mock(UserUsageGoalTimeService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://example.com/profile.png"));

        mockMvc = MockMvcBuilders.standaloneSetup(new UserUsageGoalTimeController(userUsageGoalTimeService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 목표시간을_설정하면_생성된_목표시간_이력을_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = setRequestFields();
        FieldDescriptor[] responseFieldDescriptors = setResponseFields();
        UserUsageGoalTimesSetRequest request = new UserUsageGoalTimesSetRequest(List.of(
                new UserUsageGoalTimeRequest(UsageGoalTypeCode.TOTAL_USAGE, 60),
                new UserUsageGoalTimeRequest(UsageGoalTypeCode.INSTAGRAM, 30)
        ));

        when(userUsageGoalTimeService.setGoalTimes(1L, request)).thenReturn(new UserUsageGoalTimesSetResponse(List.of(
                new UsageGoalTimeResponse(101L, UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 29, 10, 30)),
                new UsageGoalTimeResponse(102L, UsageGoalTypeCode.INSTAGRAM, 30, LocalDateTime.of(2026, 4, 29, 10, 30))
        )));

        mockMvc.perform(post("/me/usage-goal-times")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goals": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "goalMinutes": 60
                                    },
                                    {
                                      "usageGoalType": "INSTAGRAM",
                                      "goalMinutes": 30
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.goals[0].id").value(101))
                .andExpect(jsonPath("$.goals[0].usageGoalType").value("TOTAL_USAGE"))
                .andExpect(jsonPath("$.goals[0].goalMinutes").value(60))
                .andExpect(jsonPath("$.goals[0].createdAt").value("2026-04-29T10:30:00"))
                .andExpect(jsonPath("$.goals[1].id").value(102))
                .andDo(document("usage-goal-times/set",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Usage Goal Time")
                                .summary("Set my usage goal times")
                                .description("요청에 포함된 목표 타입별 목표시간을 새 이력 row로 추가한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("UserUsageGoalTimesSetRequest"))
                                .responseSchema(schema("UserUsageGoalTimesSetResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 현재_목표시간을_조회하면_목표타입별_최신값을_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] responseFieldDescriptors = currentResponseFields();

        when(userUsageGoalTimeService.getCurrentGoalTimes(1L)).thenReturn(new CurrentUsageGoalTimesResponse(List.of(
                new CurrentUsageGoalTimeResponse(101L, UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 29, 10, 30)),
                new CurrentUsageGoalTimeResponse(102L, UsageGoalTypeCode.INSTAGRAM, 30, LocalDateTime.of(2026, 4, 29, 10, 30))
        )));

        mockMvc.perform(get("/me/usage-goal-times/current")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.goals[0].id").value(101))
                .andExpect(jsonPath("$.goals[0].usageGoalType").value("TOTAL_USAGE"))
                .andExpect(jsonPath("$.goals[0].goalMinutes").value(60))
                .andExpect(jsonPath("$.goals[0].createdAt").value("2026-04-29T10:30:00"))
                .andExpect(jsonPath("$.goals[1].id").value(102))
                .andExpect(jsonPath("$.goals[1].usageGoalType").value("INSTAGRAM"))
                .andDo(document("usage-goal-times/current",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Usage Goal Time")
                                .summary("Get my current usage goal times")
                                .description("로그인 사용자의 목표 타입별 최신 목표시간을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .responseSchema(schema("CurrentUsageGoalTimesResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 목표시간_설정_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/me/usage-goal-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goals": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "goalMinutes": 60
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(userUsageGoalTimeService);
    }

    @Test
    void 목표시간_설정_요청의_goals가_비어있으면_400_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/me/usage-goal-times")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goals": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(userUsageGoalTimeService);
    }

    @Test
    void 목표시간_설정_요청의_goalMinutes가_범위를_벗어나면_400_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/me/usage-goal-times")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goals": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "goalMinutes": 1441
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(userUsageGoalTimeService);
    }

    @Test
    void 목표시간_설정_요청에_같은_목표타입이_중복되면_400_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/me/usage-goal-times")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goals": [
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "goalMinutes": 60
                                    },
                                    {
                                      "usageGoalType": "TOTAL_USAGE",
                                      "goalMinutes": 30
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(userUsageGoalTimeService);
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] setRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("goals")
                        .type(JsonFieldType.ARRAY)
                        .description("새 이력으로 추가할 목표시간 배열"),
                fieldWithPath("goals[].usageGoalType")
                        .type(JsonFieldType.STRING)
                        .description("사용시간 목표 타입 (`TOTAL_USAGE`, `INSTAGRAM`, `YOUTUBE`)"),
                fieldWithPath("goals[].goalMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("목표 사용시간(분). 0 이상 1440 이하")
        };
    }

    private FieldDescriptor[] setResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("goals")
                        .type(JsonFieldType.ARRAY)
                        .description("생성된 목표시간 이력 배열"),
                fieldWithPath("goals[].id")
                        .type(JsonFieldType.NUMBER)
                        .description("생성된 user usage goal time ID"),
                fieldWithPath("goals[].usageGoalType")
                        .type(JsonFieldType.STRING)
                        .description("사용시간 목표 타입"),
                fieldWithPath("goals[].goalMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("목표 사용시간(분)"),
                fieldWithPath("goals[].createdAt")
                        .type(JsonFieldType.STRING)
                        .description("목표시간 이력 생성 시각")
        };
    }

    private FieldDescriptor[] currentResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("goals")
                        .type(JsonFieldType.ARRAY)
                        .description("목표 타입별 최신 목표시간 배열"),
                fieldWithPath("goals[].id")
                        .type(JsonFieldType.NUMBER)
                        .description("현재 목표시간 이력 ID"),
                fieldWithPath("goals[].usageGoalType")
                        .type(JsonFieldType.STRING)
                        .description("사용시간 목표 타입"),
                fieldWithPath("goals[].goalMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("현재 목표 사용시간(분)"),
                fieldWithPath("goals[].createdAt")
                        .type(JsonFieldType.STRING)
                        .description("현재 목표시간 이력 생성 시각")
        };
    }
}
