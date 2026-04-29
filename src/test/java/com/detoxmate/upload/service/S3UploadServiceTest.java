package com.detoxmate.upload.service;

import com.detoxmate.upload.config.StorageS3Properties;
import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3UploadServiceTest {

    private final S3Presigner s3Presigner = mock(S3Presigner.class);
    private final StorageS3Properties storageS3Properties =
            new StorageS3Properties("ap-northeast-2", "detoxmate-media-dev", 600);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-04-28T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final UploadContentTypePolicy uploadContentTypePolicy = new UploadContentTypePolicy();
    private final UploadFileSizePolicy uploadFileSizePolicy = new UploadFileSizePolicy();
    private final UploadObjectKeyFactory uploadObjectKeyFactory = new UploadObjectKeyFactory(clock);
    private final S3UploadService uploadService =
            new S3UploadService(
                    s3Presigner,
                    storageS3Properties,
                    uploadContentTypePolicy,
                    uploadFileSizePolicy,
                    uploadObjectKeyFactory
            );

    @Test
    void activity_record_image용_presigned_url을_발급한다() throws MalformedURLException {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create(
                "https://detoxmate-media-dev.s3.ap-northeast-2.amazonaws.com/activity-records/7/2026/04/mock.png?signature=test"
        ).toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        PresignedUrlResponse response = uploadService.issuePresignedUrl(
                7L,
                new PresignedUrlRequest("walk photo.png", "image/png", 10L * 1024 * 1024, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        );

        assertThat(response.uploadUrl())
                .isEqualTo("https://detoxmate-media-dev.s3.ap-northeast-2.amazonaws.com/activity-records/7/2026/04/mock.png?signature=test");
        assertThat(response.objectKey())
                .startsWith("activity-records/7/2026/04/")
                .endsWith("-walk-photo.png");
        assertThat(response.expiresInSeconds()).isEqualTo(600);

        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void 아이폰_heic_이미지에_대해서도_presigned_url을_발급한다() throws MalformedURLException {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create(
                "https://detoxmate-media-dev.s3.ap-northeast-2.amazonaws.com/activity-records/7/2026/04/mock.heic?signature=test"
        ).toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        PresignedUrlResponse response = uploadService.issuePresignedUrl(
                7L,
                new PresignedUrlRequest("IMG_0001.HEIC", "image/heic", 10L * 1024 * 1024, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        );

        assertThat(response.objectKey())
                .startsWith("activity-records/7/2026/04/")
                .endsWith("-IMG_0001.HEIC");
    }

    @Test
    void 지원하지_않는_contentType이면_400_에러를_반환한다() {
        assertThatThrownBy(() -> uploadService.issuePresignedUrl(
                7L,
                new PresignedUrlRequest("walk-photo.gif", "image/gif", 1024L, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        )).isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void 대소문자가_다른_contentType이면_400_에러를_반환한다() {
        assertThatThrownBy(() -> uploadService.issuePresignedUrl(
                7L,
                new PresignedUrlRequest("walk-photo.png", "IMAGE/PNG", 1024L, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        )).isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void activity_record_image가_10MB를_초과하면_400_에러를_반환한다() {
        assertThatThrownBy(() -> uploadService.issuePresignedUrl(
                7L,
                new PresignedUrlRequest("walk-photo.png", "image/png", 10L * 1024 * 1024 + 1, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        )).isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void presigned_url_발급시_bucket_key_contentType과_만료시간을_함께_서명한다() throws MalformedURLException {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create(
                "https://detoxmate-media-dev.s3.ap-northeast-2.amazonaws.com/activity-records/7/2026/04/mock.png?signature=test"
        ).toURL());
        PutObjectPresignRequest[] capturedRequest = new PutObjectPresignRequest[1];
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenAnswer(invocation -> {
            capturedRequest[0] = invocation.getArgument(0);
            return presignedRequest;
        });

        uploadService.issuePresignedUrl(
                7L,
                new PresignedUrlRequest("walk-photo.png", "image/png", 10L * 1024 * 1024, UploadPurpose.ACTIVITY_RECORD_IMAGE)
        );

        PutObjectPresignRequest presignRequest = capturedRequest[0];
        PutObjectRequest putObjectRequest = presignRequest.putObjectRequest();

        assertThat(presignRequest.signatureDuration()).isEqualTo(Duration.ofSeconds(600));
        assertThat(putObjectRequest.bucket()).isEqualTo("detoxmate-media-dev");
        assertThat(putObjectRequest.key()).startsWith("activity-records/7/2026/04/");
        assertThat(putObjectRequest.contentType()).isEqualTo("image/png");
    }
}
