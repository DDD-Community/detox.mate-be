package com.detoxmate.upload.service;

import com.detoxmate.upload.config.StorageS3Properties;
import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class S3UploadServiceCharacterizationTest {

    private static final long MB = 1024L * 1024L;
    private static final String PRESIGNED_URL = "https://example.com/presigned-upload-url";

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-04-28T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final StorageS3Properties storageS3Properties =
            new StorageS3Properties("ap-northeast-2", "test-bucket", 600);

    private S3Presigner s3Presigner;
    private S3UploadService uploadService;

    @BeforeEach
    void setUp() throws Exception {
        s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create(PRESIGNED_URL).toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        uploadService = new S3UploadService(
                s3Presigner,
                storageS3Properties,
                new UploadContentTypePolicy(),
                uploadPurposePolicies()
        );
    }

    @Test
    @DisplayName("활동 기록 이미지 presigned URL은 기존 object key 형식을 유지한다")
    void issuePresignedUrl_keepsActivityRecordImageObjectKeyFormat() {
        // given
        PresignedUrlRequest request =
                request("walk photo.png", "image/png", 10L * MB, UploadPurpose.ACTIVITY_RECORD_IMAGE);

        // when
        PresignedUrlResponse response = uploadService.issuePresignedUrl(
                7L,
                request
        );

        // then
        assertThat(response.uploadUrl()).isEqualTo(PRESIGNED_URL);
        assertThat(response.objectKey())
                .startsWith("activity-records/7/2026/04/")
                .endsWith("-walk-photo.png");
        assertThat(response.expiresInSeconds()).isEqualTo(600);

        PutObjectRequest putObjectRequest = capturedPutObjectRequest();
        assertThat(putObjectRequest.bucket()).isEqualTo("test-bucket");
        assertThat(putObjectRequest.key()).isEqualTo(response.objectKey());
        assertThat(putObjectRequest.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("프로필 이미지 presigned URL은 기존 object key 형식을 유지한다")
    void issuePresignedUrl_keepsProfileImageObjectKeyFormat() {
        // given
        PresignedUrlRequest request =
                request("avatar.png", "image/heic", 5L * MB, UploadPurpose.PROFILE_IMAGE);

        // when
        PresignedUrlResponse response = uploadService.issuePresignedUrl(
                7L,
                request
        );

        // then
        assertThat(response.uploadUrl()).isEqualTo(PRESIGNED_URL);
        assertThat(response.objectKey())
                .startsWith("profile-images/7/")
                .endsWith("-avatar.png");
        assertThat(response.expiresInSeconds()).isEqualTo(600);

        PutObjectRequest putObjectRequest = capturedPutObjectRequest();
        assertThat(putObjectRequest.bucket()).isEqualTo("test-bucket");
        assertThat(putObjectRequest.key()).isEqualTo(response.objectKey());
        assertThat(putObjectRequest.contentType()).isEqualTo("image/heic");
    }

    @Test
    @DisplayName("스크린 타임 OCR 리포트 이미지 presigned URL은 기존 object key 형식을 유지한다")
    void issuePresignedUrl_keepsScreenTimeOcrReportImageObjectKeyFormat() {
        // given
        PresignedUrlRequest request =
                request("screen time.png", "image/png", 10L * MB, UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE);

        // when
        PresignedUrlResponse response = uploadService.issuePresignedUrl(
                7L,
                request
        );

        // then
        assertThat(response.uploadUrl()).isEqualTo(PRESIGNED_URL);
        assertThat(response.objectKey())
                .startsWith("screen-time-ocr-reports/7/2026/04/")
                .endsWith("-screen-time.png");
        assertThat(response.expiresInSeconds()).isEqualTo(600);

        PutObjectRequest putObjectRequest = capturedPutObjectRequest();
        assertThat(putObjectRequest.bucket()).isEqualTo("test-bucket");
        assertThat(putObjectRequest.key()).isEqualTo(response.objectKey());
        assertThat(putObjectRequest.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("프로필 이미지는 5MB까지 presigned URL을 발급한다")
    void issuePresignedUrl_allowsProfileImageUpTo5MB() {
        // given
        PresignedUrlRequest request =
                request("avatar.png", "image/png", 5L * MB, UploadPurpose.PROFILE_IMAGE);

        // when & then
        assertThatCode(() -> uploadService.issuePresignedUrl(
                7L,
                request
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("프로필 이미지가 5MB를 초과하면 presigned URL을 발급하지 않고 400 에러를 반환한다")
    void issuePresignedUrl_rejectsProfileImageOver5MB() {
        // given
        PresignedUrlRequest request =
                request("avatar.png", "image/png", 5L * MB + 1, UploadPurpose.PROFILE_IMAGE);

        // when & then
        assertBadRequest(() -> uploadService.issuePresignedUrl(
                7L,
                request
        ));
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("활동 기록 이미지는 10MB까지 presigned URL을 발급한다")
    void issuePresignedUrl_allowsActivityRecordImageUpTo10MB() {
        // given
        PresignedUrlRequest request =
                request("walk-photo.png", "image/png", 10L * MB, UploadPurpose.ACTIVITY_RECORD_IMAGE);

        // when & then
        assertThatCode(() -> uploadService.issuePresignedUrl(
                7L,
                request
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("활동 기록 이미지가 10MB를 초과하면 presigned URL을 발급하지 않고 400 에러를 반환한다")
    void issuePresignedUrl_rejectsActivityRecordImageOver10MB() {
        // given
        PresignedUrlRequest request =
                request("walk-photo.png", "image/png", 10L * MB + 1, UploadPurpose.ACTIVITY_RECORD_IMAGE);

        // when & then
        assertBadRequest(() -> uploadService.issuePresignedUrl(
                7L,
                request
        ));
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("스크린 타임 OCR 리포트 이미지는 10MB까지 presigned URL을 발급한다")
    void issuePresignedUrl_allowsScreenTimeOcrReportImageUpTo10MB() {
        // given
        PresignedUrlRequest request =
                request("screen-time.png", "image/png", 10L * MB, UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE);

        // when & then
        assertThatCode(() -> uploadService.issuePresignedUrl(
                7L,
                request
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("스크린 타임 OCR 리포트 이미지가 10MB를 초과하면 presigned URL을 발급하지 않고 400 에러를 반환한다")
    void issuePresignedUrl_rejectsScreenTimeOcrReportImageOver10MB() {
        // given
        PresignedUrlRequest request =
                request("screen-time.png", "image/png", 10L * MB + 1, UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE);

        // when & then
        assertBadRequest(() -> uploadService.issuePresignedUrl(
                7L,
                request
        ));
        verifyNoInteractions(s3Presigner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/heic", "image/heif"})
    @DisplayName("허용된 content type은 presigned put object request에 그대로 반영한다")
    void issuePresignedUrl_reflectsAllowedContentType(String contentType) {
        // given
        PresignedUrlRequest request =
                request("avatar.png", contentType, 1L * MB, UploadPurpose.PROFILE_IMAGE);

        // when
        uploadService.issuePresignedUrl(
                7L,
                request
        );

        // then
        assertThat(capturedPutObjectRequest().contentType()).isEqualTo(contentType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IMAGE/PNG", "image/gif"})
    @DisplayName("허용되지 않은 content type은 presigned URL을 발급하지 않고 400 에러를 반환한다")
    void issuePresignedUrl_rejectsDisallowedContentType(String contentType) {
        // given
        PresignedUrlRequest request =
                request("avatar.png", contentType, 1L * MB, UploadPurpose.PROFILE_IMAGE);

        // when & then
        assertBadRequest(() -> uploadService.issuePresignedUrl(
                7L,
                request
        ));
        verifyNoInteractions(s3Presigner);
    }

    private PresignedUrlRequest request(
            String fileName,
            String contentType,
            Long fileSize,
            UploadPurpose uploadPurpose
    ) {
        return new PresignedUrlRequest(fileName, contentType, fileSize, uploadPurpose);
    }

    private List<UploadPurposePolicy> uploadPurposePolicies() {
        return List.of(
                new ProfileImageUploadPurposePolicy(),
                new ActivityRecordImageUploadPurposePolicy(clock),
                new ScreenTimeOcrReportImageUploadPurposePolicy(clock)
        );
    }

    private PutObjectRequest capturedPutObjectRequest() {
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        return captor.getValue().putObjectRequest();
    }

    private void assertBadRequest(ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
