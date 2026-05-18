package com.detoxmate.user.controller;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.detoxmate.auth.dto.AppleSocialLoginRequest;
import com.detoxmate.auth.dto.AuthLoginResponse;
import com.detoxmate.auth.dto.RefreshTokenResponse;
import com.detoxmate.user.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(RestDocumentationExtension.class)
class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void providerAccessToken이_없으면_400_에러를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/social/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(authService);
    }

    @Test
    void providerAccessToken이_있으면_로그인_응답을_반환한다() throws Exception {
        // given
        when(authService.loginWithKakao("kakao-access-token")).thenReturn(new AuthLoginResponse(
                1L,
                "카카오닉네임",
                null,
                "service-access-token",
                "service-refresh-token",
                false
        ));

        FieldDescriptor[] requestFieldDescriptors = kakaoLoginRequestFields();
        FieldDescriptor[] responseFieldDescriptors = kakaoLoginSuccessResponseFields();

        // when & then
        mockMvc.perform(post("/auth/social/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "providerAccessToken": "kakao-access-token"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isNewUser").value(false))
                .andDo(document("auth/social/kakao",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("Kakao social login")
                                .description("카카오 access token으로 서비스 access token과 refresh token을 발급한다.")
                                .requestSchema(schema("KakaoSocialLoginRequest"))
                                .responseSchema(schema("AuthLoginResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void providerAccessToken이_공백이면_400_에러를_반환한다() throws Exception {
        FieldDescriptor[] requestFieldDescriptors = kakaoLoginRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        // when & then
        mockMvc.perform(post("/auth/social/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "providerAccessToken": "   "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("auth/social/kakao-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("Kakao social login")
                                .description("카카오 access token으로 서비스 access token과 refresh token을 발급한다.")
                                .requestSchema(schema("KakaoSocialLoginRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Apple 로그인 요청이 유효하면 로그인 응답을 반환한다")
    void appleAuth_returnsLoginResponseWhenRequestIsValid() throws Exception {
        // given
        when(authService.loginWithApple(any(AppleSocialLoginRequest.class))).thenReturn(new AuthLoginResponse(
                1L,
                "애플유저",
                null,
                "service-access-token",
                "service-refresh-token",
                true
        ));

        FieldDescriptor[] requestFieldDescriptors = appleLoginRequestFields();
        FieldDescriptor[] responseFieldDescriptors = kakaoLoginSuccessResponseFields();

        // when & then
        mockMvc.perform(post("/auth/social/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "identityToken": "apple-id-token-jwt",
                          "rawNonce": "apple-raw-nonce",
                          "displayName": "애플유저"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isNewUser").value(true))
                .andExpect(jsonPath("$.accessToken").value("service-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("service-refresh-token"))
                .andDo(document("auth/social/apple",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("Apple social login")
                                .description("Apple identity token과 raw nonce로 서비스 access token과 refresh token을 발급한다.")
                                .requestSchema(schema("AppleSocialLoginRequest"))
                                .responseSchema(schema("AuthLoginResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));

        verify(authService).loginWithApple(argThat(request ->
                request.identityToken().equals("apple-id-token-jwt")
                        && request.rawNonce().equals("apple-raw-nonce")
                        && request.displayName().equals("애플유저")
        ));
    }

    @Test
    @DisplayName("Apple 로그인 필수값이 공백이면 400 에러를 반환한다")
    void appleAuth_returnsBadRequestWhenRequiredFieldsAreBlank() throws Exception {
        FieldDescriptor[] requestFieldDescriptors = appleLoginRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        // when & then
        mockMvc.perform(post("/auth/social/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "identityToken": "   ",
                          "rawNonce": "   ",
                          "displayName": null
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("auth/social/apple-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("Apple social login")
                                .description("Apple identity token과 raw nonce로 서비스 access token과 refresh token을 발급한다.")
                                .requestSchema(schema("AppleSocialLoginRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));

        verifyNoInteractions(authService);
    }

    @Test
    void fresh한_refresh_token이_있으면_새로운_access_token과_refresh_token을_반환한다() throws Exception {
        // given
        when(authService.refresh("fresh-refresh-token")).thenReturn(new RefreshTokenResponse(
                "service-access-token",
                "rotated-refresh-token"
        ));

        FieldDescriptor[] requestFieldDescriptors = refreshTokenRequestFields();
        FieldDescriptor[] responseFieldDescriptors = refreshTokenResponseFields();

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "refreshToken": "fresh-refresh-token"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("service-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("rotated-refresh-token"))
                .andDo(document("auth/refresh",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("Refresh access token")
                                .description("refresh token으로 access token과 refresh token을 재발급한다.")
                                .requestSchema(schema("RefreshTokenRequest"))
                                .responseSchema(schema("RefreshTokenResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void refresh_token이_공백이면_400_에러를_반환한다() throws Exception {
        FieldDescriptor[] requestFieldDescriptors = refreshTokenRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "refreshToken": "   "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("auth/refresh-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("Refresh access token")
                                .description("refresh token으로 access token과 refresh token을 재발급한다.")
                                .requestSchema(schema("RefreshTokenRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));

        verifyNoInteractions(authService);
    }

    @Test
    void refresh_token이_있으면_로그아웃을_수행한다() throws Exception {
        FieldDescriptor[] requestFieldDescriptors = refreshTokenRequestFields();

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "refreshToken": "logout-refresh-token"
                        }
                        """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andDo(document("auth/logout",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("Logout")
                                .description("refresh token 세션을 만료시켜 로그아웃한다.")
                                .requestSchema(schema("RefreshTokenRequest"))
                                .requestFields(requestFieldDescriptors)
                                .build()
                        )));

        verify(authService).logout("logout-refresh-token");
    }

    @Test
    void logout_refresh_token이_공백이면_400_에러를_반환한다() throws Exception {
        FieldDescriptor[] requestFieldDescriptors = refreshTokenRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "refreshToken": "   "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("auth/logout-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("Logout")
                                .description("refresh token 세션을 만료시켜 로그아웃한다.")
                                .requestSchema(schema("RefreshTokenRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));

        verifyNoInteractions(authService);
    }

    private FieldDescriptor[] kakaoLoginRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("providerAccessToken")
                        .type(JsonFieldType.STRING)
                        .description("카카오 OAuth access token")
        };
    }

    private FieldDescriptor[] appleLoginRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("identityToken")
                        .type(JsonFieldType.STRING)
                        .description("Apple SDK가 반환한 ID token JWT"),
                fieldWithPath("rawNonce")
                        .type(JsonFieldType.STRING)
                        .description("FE가 로그인 요청마다 생성한 일회용 랜덤 문자열"),
                fieldWithPath("displayName")
                        .type(JsonFieldType.STRING)
                        .optional()
                        .description("Apple SDK에서 얻은 사용자 이름. 최초 동의 이후에는 없을 수 있음")
        };
    }

    private FieldDescriptor[] kakaoLoginSuccessResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("서비스 사용자 ID"),
                fieldWithPath("displayName")
                        .type(JsonFieldType.STRING)
                        .description("사용자 닉네임"),
                fieldWithPath("profileImageUrl")
                        .type(JsonFieldType.STRING)
                        .optional()
                        .description("저장된 프로필 이미지 object key를 읽기 URL로 변환한 값"),
                fieldWithPath("accessToken")
                        .type(JsonFieldType.STRING)
                        .description("서비스 access token"),
                fieldWithPath("refreshToken")
                        .type(JsonFieldType.STRING)
                        .description("서비스 refresh token"),
                fieldWithPath("isNewUser")
                        .type(JsonFieldType.BOOLEAN)
                        .description("신규 가입 사용자 여부")
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

    private FieldDescriptor[] refreshTokenRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("refreshToken")
                        .type(JsonFieldType.STRING)
                        .description("서비스 refresh token")
        };
    }

    private FieldDescriptor[] refreshTokenResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("accessToken")
                        .type(JsonFieldType.STRING)
                        .description("새로 발급된 서비스 access token"),
                fieldWithPath("refreshToken")
                        .type(JsonFieldType.STRING)
                        .description("새로 발급된 서비스 refresh token")
        };
    }
}
