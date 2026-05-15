package com.detoxmate.upload.service;

import com.detoxmate.upload.config.StorageS3Properties;
import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.BeforeEach;
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
    void activity_record_image_presigned_url은_기존_object_key_형식을_유지한다() {
        PresignedUrlResponse response = uploadService.issuePresignedUrl(
                7L,
                request("walk photo.png", "image/png", 10L * MB, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        );

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
    void profile_image_presigned_url은_기존_object_key_형식을_유지한다() {
        PresignedUrlResponse response = uploadService.issuePresignedUrl(
                7L,
                request("avatar.png", "image/heic", 5L * MB, UploadPurpose.PROFILE_IMAGE)
        );

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
    void screen_time_ocr_report_image_presigned_url은_기존_object_key_형식을_유지한다() {
        PresignedUrlResponse response = uploadService.issuePresignedUrl(
                7L,
                request("screen time.png", "image/png", 10L * MB, UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE)
        );

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
    void profile_image는_5MB까지_presigned_url을_발급한다() {
        assertThatCode(() -> uploadService.issuePresignedUrl(
                7L,
                request("avatar.png", "image/png", 5L * MB, UploadPurpose.PROFILE_IMAGE)
        )).doesNotThrowAnyException();
    }

    @Test
    void profile_image가_5MB를_초과하면_presigned_url을_발급하지_않고_400_에러를_반환한다() {
        assertBadRequest(() -> uploadService.issuePresignedUrl(
                7L,
                request("avatar.png", "image/png", 5L * MB + 1, UploadPurpose.PROFILE_IMAGE)
        ));
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void activity_record_image는_10MB까지_presigned_url을_발급한다() {
        assertThatCode(() -> uploadService.issuePresignedUrl(
                7L,
                request("walk-photo.png", "image/png", 10L * MB, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        )).doesNotThrowAnyException();
    }

    @Test
    void activity_record_image가_10MB를_초과하면_presigned_url을_발급하지_않고_400_에러를_반환한다() {
        assertBadRequest(() -> uploadService.issuePresignedUrl(
                7L,
                request("walk-photo.png", "image/png", 10L * MB + 1, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        ));
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void screen_time_ocr_report_image는_10MB까지_presigned_url을_발급한다() {
        assertThatCode(() -> uploadService.issuePresignedUrl(
                7L,
                request("screen-time.png", "image/png", 10L * MB, UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE)
        )).doesNotThrowAnyException();
    }

    @Test
    void screen_time_ocr_report_image가_10MB를_초과하면_presigned_url을_발급하지_않고_400_에러를_반환한다() {
        assertBadRequest(() -> uploadService.issuePresignedUrl(
                7L,
                request("screen-time.png", "image/png", 10L * MB + 1, UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE)
        ));
        verifyNoInteractions(s3Presigner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/heic", "image/heif"})
    void 허용된_contentType은_presigned_put_object_request에_그대로_반영한다(String contentType) {
        uploadService.issuePresignedUrl(
                7L,
                request("avatar.png", contentType, 1L * MB, UploadPurpose.PROFILE_IMAGE)
        );

        assertThat(capturedPutObjectRequest().contentType()).isEqualTo(contentType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IMAGE/PNG", "image/gif"})
    void 허용되지_않은_contentType은_presigned_url을_발급하지_않고_400_에러를_반환한다(String contentType) {
        assertBadRequest(() -> uploadService.issuePresignedUrl(
                7L,
                request("avatar.png", contentType, 1L * MB, UploadPurpose.PROFILE_IMAGE)
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

    private java.util.List<UploadPurposePolicy> uploadPurposePolicies() {
        return java.util.List.of(
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
