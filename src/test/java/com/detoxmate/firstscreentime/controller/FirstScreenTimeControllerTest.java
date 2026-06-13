package com.detoxmate.firstscreentime.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.common.error.GlobalExceptionHandlerTestFixture;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.firstscreentime.FirstScreenTimeErrorCode;
import com.detoxmate.firstscreentime.dto.FirstScreenTimeCreateRequest;
import com.detoxmate.firstscreentime.dto.FirstScreenTimeResponse;
import com.detoxmate.firstscreentime.service.FirstScreenTimeService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class FirstScreenTimeControllerTest {

    private FirstScreenTimeService firstScreenTimeService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        firstScreenTimeService = mock(FirstScreenTimeService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://example.com/profile.png", true));

        mockMvc = MockMvcBuilders.standaloneSetup(new FirstScreenTimeController(firstScreenTimeService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(GlobalExceptionHandlerTestFixture.globalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    @DisplayName("첫 스크린타임을 저장하면 저장 결과를 반환한다")
    void create_returnsCreatedFirstScreenTime() throws Exception {
        // given
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = createRequestFields();
        FieldDescriptor[] responseFieldDescriptors = firstScreenTimeResponseFields();

        when(firstScreenTimeService.create(1L, new FirstScreenTimeCreateRequest(
                10L,
                180,
                LocalDate.of(2026, 6, 11)
        ))).thenReturn(firstScreenTimeResponse());

        // when & then
        mockMvc.perform(post("/first-screen-times")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupChallengeParticipantId": 10,
                                  "screenTimeMinutes": 180,
                                  "recordDate": "2026-06-11"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.groupChallengeParticipantId").value(10))
                .andExpect(jsonPath("$.screenTimeMinutes").value(180))
                .andExpect(jsonPath("$.recordDate").value("2026-06-11"))
                .andExpect(jsonPath("$.createdAt").value("2026-06-11T21:30:00"))
                .andDo(document("first-screen-times/create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("First Screen Time")
                                .summary("Create first screen time")
                                .description("그룹 챌린지 참여자의 첫 스크린타임을 1회 저장한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("FirstScreenTimeCreateRequest"))
                                .responseSchema(schema("FirstScreenTimeResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    @DisplayName("첫 스크린타임 저장 요청값이 유효하지 않으면 400 에러를 반환한다")
    void create_returnsBadRequestWhenRequestIsInvalid() throws Exception {
        // given
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = createRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        // when & then
        mockMvc.perform(post("/first-screen-times")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupChallengeParticipantId": 10,
                                  "screenTimeMinutes": -1,
                                  "recordDate": "2026-06-11"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("first-screen-times/create-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("First Screen Time")
                                .summary("Create first screen time")
                                .description("그룹 챌린지 참여자의 첫 스크린타임을 1회 저장한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("FirstScreenTimeCreateRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));

        verifyNoInteractions(firstScreenTimeService);
    }

    @Test
    @DisplayName("참여 중인 챌린지가 아니면 첫 스크린타임 저장 시 403 에러를 반환한다")
    void create_returnsForbiddenWhenParticipantIsNotActiveForCurrentUser() throws Exception {
        // given
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = createRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        FirstScreenTimeCreateRequest request = new FirstScreenTimeCreateRequest(
                10L,
                180,
                LocalDate.of(2026, 6, 11)
        );
        when(firstScreenTimeService.create(1L, request))
                .thenThrow(new CustomException(FirstScreenTimeErrorCode.CAN_REGISTER_PARTICIPATE_CHALLENGE));

        // when & then
        mockMvc.perform(post("/first-screen-times")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupChallengeParticipantId": 10,
                                  "screenTimeMinutes": 180,
                                  "recordDate": "2026-06-11"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CAN_REGISTER_PARTICIPATE_CHALLENGE"))
                .andExpect(jsonPath("$.status").value(403))
                .andDo(document("first-screen-times/create-forbidden",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("First Screen Time")
                                .summary("Create first screen time")
                                .description("그룹 챌린지 참여자의 첫 스크린타임을 1회 저장한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("FirstScreenTimeCreateRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    @DisplayName("이미 첫 스크린타임이 있으면 저장 시 409 에러를 반환한다")
    void create_returnsConflictWhenFirstScreenTimeAlreadyExists() throws Exception {
        // given
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = createRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        FirstScreenTimeCreateRequest request = new FirstScreenTimeCreateRequest(
                10L,
                180,
                LocalDate.of(2026, 6, 11)
        );
        when(firstScreenTimeService.create(1L, request))
                .thenThrow(new CustomException(FirstScreenTimeErrorCode.FIRST_SCREEN_TIME_ALREADY_REGISTER));

        // when & then
        mockMvc.perform(post("/first-screen-times")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupChallengeParticipantId": 10,
                                  "screenTimeMinutes": 180,
                                  "recordDate": "2026-06-11"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FIRST_SCREEN_TIME_ALREADY_REGISTER"))
                .andExpect(jsonPath("$.status").value(409))
                .andDo(document("first-screen-times/create-conflict",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("First Screen Time")
                                .summary("Create first screen time")
                                .description("그룹 챌린지 참여자의 첫 스크린타임을 1회 저장한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("FirstScreenTimeCreateRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    @DisplayName("저장된 첫 스크린타임을 조회하면 첫 스크린타임 정보를 반환한다")
    void get_returnsFirstScreenTime() throws Exception {
        // given
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = pathParameterDescriptors();
        FieldDescriptor[] responseFieldDescriptors = firstScreenTimeResponseFields();

        when(firstScreenTimeService.get(1L, 10L)).thenReturn(firstScreenTimeResponse());

        // when & then
        mockMvc.perform(get("/group-challenge-participants/{groupChallengeParticipantId}/first-screen-time", 10L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.groupChallengeParticipantId").value(10))
                .andExpect(jsonPath("$.screenTimeMinutes").value(180))
                .andExpect(jsonPath("$.recordDate").value("2026-06-11"))
                .andExpect(jsonPath("$.createdAt").value("2026-06-11T21:30:00"))
                .andDo(document("group-challenge-participants/first-screen-time-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("First Screen Time")
                                .summary("Get first screen time")
                                .description("그룹 챌린지 참여자의 첫 스크린타임을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("FirstScreenTimeResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    @DisplayName("참여 중인 챌린지가 아니면 첫 스크린타임 조회 시 403 에러를 반환한다")
    void get_returnsForbiddenWhenParticipantIsNotActiveForCurrentUser() throws Exception {
        // given
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = pathParameterDescriptors();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        when(firstScreenTimeService.get(1L, 10L))
                .thenThrow(new CustomException(FirstScreenTimeErrorCode.CAN_REGISTER_PARTICIPATE_CHALLENGE));

        // when & then
        mockMvc.perform(get("/group-challenge-participants/{groupChallengeParticipantId}/first-screen-time", 10L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CAN_REGISTER_PARTICIPATE_CHALLENGE"))
                .andExpect(jsonPath("$.status").value(403))
                .andDo(document("group-challenge-participants/first-screen-time-get-forbidden",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("First Screen Time")
                                .summary("Get first screen time")
                                .description("그룹 챌린지 참여자의 첫 스크린타임을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("ErrorResponse"))
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    @DisplayName("저장된 첫 스크린타임이 없으면 조회 시 404 에러를 반환한다")
    void get_returnsNotFoundWhenFirstScreenTimeDoesNotExist() throws Exception {
        // given
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = pathParameterDescriptors();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        when(firstScreenTimeService.get(1L, 10L))
                .thenThrow(new CustomException(FirstScreenTimeErrorCode.CAN_NOT_FIND_PARTICIPATE_INFORMATION));

        // when & then
        mockMvc.perform(get("/group-challenge-participants/{groupChallengeParticipantId}/first-screen-time", 10L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CAN_NOT_FIND_PARTICIPATE_INFORMATION"))
                .andExpect(jsonPath("$.status").value(404))
                .andDo(document("group-challenge-participants/first-screen-time-get-not-found",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("First Screen Time")
                                .summary("Get first screen time")
                                .description("그룹 챌린지 참여자의 첫 스크린타임을 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("ErrorResponse"))
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] createRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("groupChallengeParticipantId")
                        .type(JsonFieldType.NUMBER)
                        .description("첫 스크린타임을 저장할 그룹 챌린지 참여자 ID"),
                fieldWithPath("screenTimeMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("분 단위 첫 스크린타임. 0 이상이어야 한다."),
                fieldWithPath("recordDate")
                        .type(JsonFieldType.STRING)
                        .description("첫 스크린타임을 기록한 날짜")
        };
    }

    private ParameterDescriptor[] pathParameterDescriptors() {
        return new ParameterDescriptor[] {
                parameterWithName("groupChallengeParticipantId")
                        .description("조회할 그룹 챌린지 참여자 ID")
        };
    }

    private FieldDescriptor[] firstScreenTimeResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("첫 스크린타임 ID"),
                fieldWithPath("groupChallengeParticipantId")
                        .type(JsonFieldType.NUMBER)
                        .description("그룹 챌린지 참여자 ID"),
                fieldWithPath("screenTimeMinutes")
                        .type(JsonFieldType.NUMBER)
                        .description("분 단위 첫 스크린타임"),
                fieldWithPath("recordDate")
                        .type(JsonFieldType.STRING)
                        .description("첫 스크린타임을 기록한 날짜"),
                fieldWithPath("createdAt")
                        .type(JsonFieldType.STRING)
                        .description("첫 스크린타임 생성 시각")
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
                        .description("HTTP status code")
        };
    }

    private FirstScreenTimeResponse firstScreenTimeResponse() {
        return new FirstScreenTimeResponse(
                100L,
                10L,
                180,
                LocalDate.of(2026, 6, 11),
                LocalDateTime.of(2026, 6, 11, 21, 30)
        );
    }
}
