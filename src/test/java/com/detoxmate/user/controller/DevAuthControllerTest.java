package com.detoxmate.user.controller;

import com.detoxmate.auth.dto.AuthLoginResponse;
import com.detoxmate.user.service.DevAuthService;
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
import org.springframework.web.server.ResponseStatusException;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
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
class DevAuthControllerTest {

    private DevAuthService devAuthService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        devAuthService = mock(DevAuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DevAuthController(devAuthService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 테스트_로그인에_성공하면_토큰을_반환한다() throws Exception {
        when(devAuthService.testLogin("front-a"))
                .thenReturn(new AuthLoginResponse(
                        1L,
                        "프론트 테스트 A",
                        null,
                        "access-token",
                        "refresh-token",
                        false
                ));
        FieldDescriptor[] requestFieldDescriptors = testLoginRequestFields();
        FieldDescriptor[] responseFieldDescriptors = testLoginResponseFields();

        mockMvc.perform(post("/dev/auth/test-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "testUserKey": "front-a"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.displayName").value("프론트 테스트 A"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.isNewUser").value(false))
                .andDo(document("dev-auth/test-login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Dev Auth")
                                .summary("Issue test user tokens")
                                .description("local/dev 환경에서 테스트 유저의 서비스 access token과 refresh token을 발급한다.")
                                .requestSchema(schema("DevTestLoginRequest"))
                                .responseSchema(schema("AuthLoginResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 테스트_유저_키가_비어있으면_400_에러를_반환한다() throws Exception {
        FieldDescriptor[] requestFieldDescriptors = testLoginRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        mockMvc.perform(post("/dev/auth/test-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "testUserKey": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("dev-auth/test-login-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Dev Auth")
                                .summary("Issue test user tokens")
                                .description("local/dev 환경에서 테스트 유저의 서비스 access token과 refresh token을 발급한다.")
                                .requestSchema(schema("DevTestLoginRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 허용되지_않은_테스트_유저_키이면_400_에러를_반환한다() throws Exception {
        when(devAuthService.testLogin("unknown"))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "Unsupported test user key"));
        FieldDescriptor[] requestFieldDescriptors = testLoginRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        mockMvc.perform(post("/dev/auth/test-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "testUserKey": "unknown"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("dev-auth/test-login-unsupported-key",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Dev Auth")
                                .summary("Issue test user tokens")
                                .description("local/dev 환경에서 테스트 유저의 서비스 access token과 refresh token을 발급한다.")
                                .requestSchema(schema("DevTestLoginRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    private FieldDescriptor[] testLoginRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("testUserKey")
                        .type(JsonFieldType.STRING)
                        .description("테스트 유저 키. 허용값: front-a, front-b, front-c, server-a, server-b, server-c")
        };
    }

    private FieldDescriptor[] testLoginResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("유저 ID"),
                fieldWithPath("displayName")
                        .type(JsonFieldType.STRING)
                        .description("유저 표시 이름"),
                fieldWithPath("profileImageUrl")
                        .type(JsonFieldType.NULL)
                        .description("저장된 프로필 이미지 object key를 읽기 URL로 변환한 값"),
                fieldWithPath("accessToken")
                        .type(JsonFieldType.STRING)
                        .description("서비스 access token"),
                fieldWithPath("refreshToken")
                        .type(JsonFieldType.STRING)
                        .description("서비스 refresh token"),
                fieldWithPath("isNewUser")
                        .type(JsonFieldType.BOOLEAN)
                        .description("신규 생성 여부")
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
