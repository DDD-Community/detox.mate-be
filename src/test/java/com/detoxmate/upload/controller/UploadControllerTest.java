package com.detoxmate.upload.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.common.error.GlobalExceptionHandler;
import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.dto.UploadPurpose;
import com.detoxmate.upload.service.UploadService;
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
class UploadControllerTest {

    private static final String TEST_UPLOAD_URL = "https://example.com/uploads/activity-records/1/2026/04/mock-walk-photo.png?signature=mock-signature";

    private UploadService uploadService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        uploadService = mock(UploadService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://example.com/profile.png", true));

        mockMvc = MockMvcBuilders.standaloneSetup(new UploadController(uploadService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void presigned_url을_발급하면_uploadUrl과_objectKey를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = presignedUrlRequestFields();
        FieldDescriptor[] responseFieldDescriptors = presignedUrlResponseFields();

        when(uploadService.issuePresignedUrl(1L, new PresignedUrlRequest(
                "walk-photo.png",
                "image/png",
                1_048_576L,
                UploadPurpose.ACTIVITY_RECORD_IMAGE
        ))).thenReturn(new PresignedUrlResponse(
                TEST_UPLOAD_URL,
                "activity-records/1/2026/04/mock-walk-photo.png",
                600
        ));

        mockMvc.perform(post("/uploads/presigned-urls")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "walk-photo.png",
                                  "contentType": "image/png",
                                  "fileSize": 1048576,
                                  "uploadPurpose": "ACTIVITY_RECORD_IMAGE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uploadUrl").value(TEST_UPLOAD_URL))
                .andExpect(jsonPath("$.objectKey").value("activity-records/1/2026/04/mock-walk-photo.png"))
                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                .andDo(document("uploads/presigned-urls",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Upload")
                                .summary("Issue presigned URL")
                                .description("업로드 목적에 맞는 presigned URL과 object key를 발급한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("PresignedUrlRequest"))
                                .responseSchema(schema("PresignedUrlResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void presigned_url_발급_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/uploads/presigned-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "walk-photo.png",
                                  "contentType": "image/png",
                                  "fileSize": 1048576,
                                  "uploadPurpose": "ACTIVITY_RECORD_IMAGE"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(uploadService);
    }

    @Test
    void fileName이_없으면_400_에러를_반환한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = presignedUrlRequestFields();
        FieldDescriptor[] errorResponseFieldDescriptors = errorResponseFields();

        mockMvc.perform(post("/uploads/presigned-urls")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": " ",
                                  "contentType": "image/png",
                                  "fileSize": 1048576,
                                  "uploadPurpose": "ACTIVITY_RECORD_IMAGE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andDo(document("uploads/presigned-urls-invalid-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        responseFields(errorResponseFieldDescriptors),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Upload")
                                .summary("Issue presigned URL")
                                .description("업로드 목적에 맞는 presigned URL과 object key를 발급한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("PresignedUrlRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .requestFields(requestFieldDescriptors)
                                .responseFields(errorResponseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void fileSize가_없으면_400_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/uploads/presigned-urls")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "walk-photo.png",
                                  "contentType": "image/png",
                                  "uploadPurpose": "ACTIVITY_RECORD_IMAGE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(uploadService);
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName("Authorization").description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] presignedUrlRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("fileName")
                        .type(JsonFieldType.STRING)
                        .description("업로드할 원본 파일명"),
                fieldWithPath("contentType")
                        .type(JsonFieldType.STRING)
                        .description("업로드 파일 content type. S3 PUT 요청에도 같은 canonical 값을 사용해야 한다."),
                fieldWithPath("fileSize")
                        .type(JsonFieldType.NUMBER)
                        .description("업로드 파일 크기(byte)"),
                fieldWithPath("uploadPurpose")
                        .type(JsonFieldType.STRING)
                        .description("업로드 목적 (`ACTIVITY_RECORD_IMAGE`, `PROFILE_IMAGE`)")
        };
    }

    private FieldDescriptor[] presignedUrlResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("uploadUrl")
                        .type(JsonFieldType.STRING)
                        .description("S3에 직접 PUT 요청할 presigned URL"),
                fieldWithPath("objectKey")
                        .type(JsonFieldType.STRING)
                        .description("최종 저장 API에 다시 전달할 object key"),
                fieldWithPath("expiresInSeconds")
                        .type(JsonFieldType.NUMBER)
                        .description("presigned URL 만료 시간(초)")
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
