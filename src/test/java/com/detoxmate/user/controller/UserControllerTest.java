package com.detoxmate.user.controller;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.nullValue;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.ArgumentMatchers.argThat;
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
                .setControllerAdvice(com.detoxmate.common.error.GlobalExceptionHandlerTestFixture.globalExceptionHandler())
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
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", "ABCDE", true));
        when(userService.getMe(1L))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", "ABCDE", true));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] responseFieldDescriptors = myPageResponseFields();

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pushNotificationEnabled").value(true))
                .andExpect(jsonPath("$.userCode").value("ABCDE"))
                .andDo(document("users/me-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("Get my profile")
                                .description("Authorization 헤더의 access token으로 사용자 ID, 닉네임, 5자리 사용자 코드, 프로필 이미지 URL과 알림 수신 여부를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .responseSchema(schema("MyPageResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    @DisplayName("프로필 응답은 이미지가 없고 알림이 꺼져 있어도 필드를 유지한다")
    void getMe_returnsNullImageAndDisabledNotifications() throws Exception {
        // given
        MyProfileResponse profile = new MyProfileResponse(1L, "사용자", null, "ABCDE", false);
        when(userService.getMe("access-token")).thenReturn(profile);
        when(userService.getMe(1L)).thenReturn(profile);

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pushNotificationEnabled").value(false))
                .andExpect(jsonPath("$.userCode").value("ABCDE"))
                .andExpect(jsonPath("$.profileImageUrl").hasJsonPath())
                .andExpect(jsonPath("$.profileImageUrl").value(nullValue()))
                .andDo(org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document(
                        "users/me-get-without-image",
                        responseFields(myPageResponseFields())));
    }

    @Test
    void 내_프로필을_수정하면_수정된_유저_정보를_반환한다() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", "ABCDE", true));
        when(userService.updateMe(eq(1L), argThat(request ->
                "의진".equals(request.displayName())
                        && "profile-images/1/550e8400-e29b-41d4-a716-446655440000-profile.png"
                        .equals(request.profileImageObjectKey())
                        && request.hasProfileImageObjectKey()
        )))
                .thenReturn(new MyProfileResponse(
                        1L,
                        "의진",
                        "https://media.detoxmate.co.kr/profile-images/1/550e8400-e29b-41d4-a716-446655440000-profile.png",
                        "ABCDE",
                        true
                ));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = updateMyProfileRequestFields();
        FieldDescriptor[] responseFieldDescriptors = myPageResponseFields();

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
                .andExpect(jsonPath("$.pushNotificationEnabled").value(true))
                .andExpect(jsonPath("$.userCode").value("ABCDE"))
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
                                .description("한 요청 본문에 displayName과 profileImageObjectKey를 함께 보내면 두 값을 함께 수정한다. 생략한 필드는 유지하며 profileImageObjectKey가 null이면 이미지를 제거한다. 잘못된 이미지 경로는 두 변경 모두 롤백한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("UpdateMyProfileRequest"))
                                .responseSchema(schema("MyPageResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 내_프로필_수정_요청에서_프로필_이미지_object_key를_null로_보내면_이미지를_제거한다() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", "ABCDE", true));
        when(userService.updateMe(eq(1L), argThat(request ->
                request.displayName() == null
                        && request.profileImageObjectKey() == null
                        && request.hasProfileImageObjectKey()
        )))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", null, "ABCDE", true));

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "profileImageObjectKey": null
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pushNotificationEnabled").value(true))
                .andExpect(jsonPath("$.userCode").value("ABCDE"))
                .andExpect(jsonPath("$.profileImageUrl").value(nullValue()));
    }

    @Test
    @DisplayName("프로필 이름을 null로 보내면 기존 이름을 유지한다")
    void updateMyProfile_nullDisplayNameKeepsExistingName() throws Exception {
        // given
        MyProfileResponse profile = new MyProfileResponse(1L, "의진", null, "ABCDE", true);
        when(userService.getMe("access-token")).thenReturn(profile);
        when(userService.updateMe(eq(1L), argThat(request -> request.displayName() == null)))
                .thenReturn(profile);

        // when & then
        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("의진"));
    }

    @Test
    void 내_프로필_수정_요청의_필드가_공백이면_400_에러를_반환한다() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", "ABCDE", true));

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
    @DisplayName("내 프로필 수정 요청의 이름이 5자를 초과하면 400 에러를 반환한다")
    void updateMyProfile_rejectsDisplayNameLongerThanFiveCharacters() throws Exception {
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", "ABCDE", true));

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "displayName": "123456"
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
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임", "https://example.com/profile.png", "ABCDE", true));

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
                                .description("Authorization 헤더의 access token으로 사용자 ID, 닉네임, 5자리 사용자 코드, 프로필 이미지 URL과 알림 수신 여부를 조회한다.")
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

    private FieldDescriptor[] myPageResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("사용자 ID"),
                fieldWithPath("pushNotificationEnabled").type(JsonFieldType.BOOLEAN).description("알림 수신 여부. OFF이면 false"),
                fieldWithPath("displayName")
                        .type(JsonFieldType.STRING)
                        .description("사용자 닉네임"),
                fieldWithPath("userCode")
                        .type(JsonFieldType.STRING)
                        .description("친구 검색에 사용하는 5자리 사용자 코드"),
                fieldWithPath("profileImageUrl")
                        .type(JsonFieldType.STRING)
                        .description("항상 포함되는 프로필 이미지 읽기 URL. 이미지가 없으면 null")
                        .optional()
        };
    }

    private FieldDescriptor[] updateMyProfileRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("displayName")
                        .type(JsonFieldType.STRING)
                        .description("변경할 프로필 이름. 공백 없이 유니코드 코드 포인트 기준 1자 이상 5자 이하이며 이모지와 특수문자를 허용한다. 조합 이모지는 구성 코드 포인트 수로 계산한다. 생략하거나 null로 전달하면 기존 값을 유지한다.")
                        .optional(),
                fieldWithPath("profileImageObjectKey")
                        .type(JsonFieldType.STRING)
                        .description("PROFILE_IMAGE presigned URL 발급 응답의 S3 object key. 전달하지 않으면 기존 이미지를 유지하고, null로 전달하면 프로필 이미지를 제거한다.")
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
