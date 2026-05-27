package com.detoxmate.user.controller;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.dto.UpdateMyProfileRequest;
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

import java.util.NoSuchElementException;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(RestDocumentationExtension.class)
class UserControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void Authorization_헤더가_있으면_유저_정보를_반환한다() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", true));
        when(userService.getMe(1L))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", true));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] responseFieldDescriptors = myProfileResponseFields();

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andDo(document("users/me-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("Get my profile")
                                .description("Authorization 헤더의 access token으로 내 프로필 정보를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .responseSchema(schema("MyProfileResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 내_프로필을_수정하면_수정된_유저_정보를_반환한다() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", true));
        UpdateMyProfileRequest request = new UpdateMyProfileRequest(
                "의진",
                "profile-images/1/550e8400-e29b-41d4-a716-446655440000-profile.png"
        );
        when(userService.updateMe(eq(1L), eq(request)))
                .thenReturn(new MyProfileResponse(
                        1L,
                        "의진",
                        "https://media.detoxmate.co.kr/profile-images/1/550e8400-e29b-41d4-a716-446655440000-profile.png",
                        true
                ));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = updateMyProfileRequestFields();
        FieldDescriptor[] responseFieldDescriptors = myProfileResponseFields();

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "displayName": "의진",
                          "profileImageObjectKey": "profile-images/1/550e8400-e29b-41d4-a716-446655440000-profile.png"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.displayName").value("의진"))
                .andDo(document("users/me-patch",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("Update my profile")
                                .description("로그인 사용자의 표시 이름과 프로필 이미지 object key를 부분 수정한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("UpdateMyProfileRequest"))
                                .responseSchema(schema("MyProfileResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 내_프로필_수정_요청의_필드가_공백이면_400_에러를_반환한다() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", true));

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "displayName": " ",
                          "profileImageObjectKey": " "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void 내_프로필_수정_요청의_닉네임이_10자를_초과하면_400_에러를_반환한다() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", true));

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "displayName": "12345678901"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void 회원_탈퇴를_요청하면_204_응답을_반환한다() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", true));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(result -> verify(userService).withdrawMe(1L))
                .andDo(document("users/me-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("Withdraw my account")
                                .description("로그인 사용자의 소셜 provider 연결을 해제한 뒤 회원 탈퇴를 수행한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .build()
                        )));
    }

    @Test
    void 잘못된_JWT로_회원_탈퇴를_요청하면_401_에러를_반환한다() throws Exception {
        when(userService.getMe("invalid-token")).thenThrow(new JwtException("invalid jwt"));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer invalid-token"))
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
                                .tag("User")
                                .summary("Withdraw my account")
                                .description("로그인 사용자의 소셜 provider 연결을 해제한 뒤 회원 탈퇴를 수행한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .responseSchema(schema("ErrorResponse"))
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 잘못된_JWT이면_401_에러를_반환한다() throws Exception {
        when(userService.getMe("invalid-token")).thenThrow(new JwtException("invalid jwt"));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andDo(document("users/me-get-unauthorized",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("Get my profile")
                                .description("Authorization 헤더의 access token으로 내 프로필 정보를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .responseSchema(schema("ErrorResponse"))
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 만료된_JWT이면_401_에러를_반환한다() throws Exception {
        when(userService.getMe("expired-token")).thenThrow(new JwtException("expired jwt"));

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 토큰은_유효하지만_유저가_없으면_401_에러를_반환한다() throws Exception {
        when(userService.getMe("missing-user-token")).thenThrow(new NoSuchElementException());

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer missing-user-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] myProfileResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("서비스 사용자 ID"),
                fieldWithPath("displayName")
                        .type(JsonFieldType.STRING)
                        .description("사용자 닉네임"),
                fieldWithPath("profileImageUrl")
                        .type(JsonFieldType.STRING)
                        .description("저장된 프로필 이미지 object key를 읽기 URL로 변환한 값")
                        .optional(),
                fieldWithPath("pushNotificationEnabled")
                        .type(JsonFieldType.BOOLEAN)
                        .description("푸시 알림 수신 여부")
                        .optional()
        };
    }

    private FieldDescriptor[] updateMyProfileRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("displayName")
                        .type(JsonFieldType.STRING)
                        .description("변경할 사용자 닉네임. 공백 포함 1자 이상 10자 이하. 전달하지 않으면 기존 값을 유지한다.")
                        .optional(),
                fieldWithPath("profileImageObjectKey")
                        .type(JsonFieldType.STRING)
                        .description("PROFILE_IMAGE presigned URL 발급 응답의 S3 object key. 전달하지 않으면 기존 이미지를 유지한다.")
                        .optional()
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
