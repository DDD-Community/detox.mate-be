package com.detoxmate.user.controller;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.detoxmate.user.service.UserService;
import io.jsonwebtoken.JwtException;
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

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class UserDevControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserDevController(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 유효한_Authorization_헤더가_있으면_회원_탈퇴에_성공한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();

        mockMvc.perform(delete("/users/me").header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andDo(document("users/me-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("User Dev")
                                .summary("Withdraw my account for dev")
                                .description("dev 환경에서 Authorization 헤더의 access token으로 회원 탈퇴를 수행한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .build()
                        )));
    }

    @Test
    void 잘못된_JWT로_회원_탈퇴를_요청하면_401_에러를_반환한다() throws Exception {
        doThrow(new JwtException("invalid jwt")).when(userService).withdraw("invalid-token");

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        mockMvc.perform(delete("/users/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andDo(document("users/me-delete-unauthorized",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("User Dev")
                                .summary("Withdraw my account for dev")
                                .description("dev 환경에서 Authorization 헤더의 access token으로 회원 탈퇴를 수행한다.")
                                .requestHeaders(requestHeaderDescriptors)
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
