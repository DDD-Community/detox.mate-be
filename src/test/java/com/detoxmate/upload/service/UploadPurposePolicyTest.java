package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadPurposePolicyTest {

    private static final long MB = 1024L * 1024L;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-04-28T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void activity_record_image는_기존_object_key_형식을_유지한다() {
        UploadPurposePolicy policy = new ActivityRecordImageUploadPurposePolicy(clock);

        String objectKey = policy.createObjectKey(7L, "walk photo.png");

        assertThat(policy.purpose()).isEqualTo(UploadPurpose.ACTIVITY_RECORD_IMAGE);
        assertThat(objectKey)
                .startsWith("activity-records/7/2026/04/")
                .endsWith("-walk-photo.png");
    }

    @Test
    void profile_image는_기존_object_key_형식을_유지한다() {
        UploadPurposePolicy policy = new ProfileImageUploadPurposePolicy();

        String objectKey = policy.createObjectKey(7L, "avatar.png");

        assertThat(policy.purpose()).isEqualTo(UploadPurpose.PROFILE_IMAGE);
        assertThat(objectKey)
                .startsWith("profile-images/7/")
                .endsWith("-avatar.png");
    }

    @Test
    void screen_time_ocr_report_image는_기존_object_key_형식을_유지한다() {
        UploadPurposePolicy policy = new ScreenTimeOcrReportImageUploadPurposePolicy(clock);

        String objectKey = policy.createObjectKey(7L, "screen time.png");

        assertThat(policy.purpose()).isEqualTo(UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE);
        assertThat(objectKey)
                .startsWith("screen-time-ocr-reports/7/2026/04/")
                .endsWith("-screen-time.png");
    }

    @Test
    void profile_image는_5MB까지_허용한다() {
        assertThatCode(() -> new ProfileImageUploadPurposePolicy().validateFileSize(5L * MB))
                .doesNotThrowAnyException();
    }

    @Test
    void activity_record_image는_10MB까지_허용한다() {
        assertThatCode(() -> new ActivityRecordImageUploadPurposePolicy(clock).validateFileSize(10L * MB))
                .doesNotThrowAnyException();
    }

    @Test
    void screen_time_ocr_report_image는_10MB까지_허용한다() {
        assertThatCode(() -> new ScreenTimeOcrReportImageUploadPurposePolicy(clock).validateFileSize(10L * MB))
                .doesNotThrowAnyException();
    }

    @Test
    void profile_image가_5MB를_초과하면_400_에러를_반환한다() {
        assertBadRequest(() -> new ProfileImageUploadPurposePolicy().validateFileSize(5L * MB + 1));
    }

    @Test
    void activity_record_image가_10MB를_초과하면_400_에러를_반환한다() {
        assertBadRequest(() -> new ActivityRecordImageUploadPurposePolicy(clock).validateFileSize(10L * MB + 1));
    }

    @Test
    void screen_time_ocr_report_image가_10MB를_초과하면_400_에러를_반환한다() {
        assertBadRequest(() -> new ScreenTimeOcrReportImageUploadPurposePolicy(clock).validateFileSize(10L * MB + 1));
    }

    @Test
    void fileSize가_양수가_아니면_400_에러를_반환한다() {
        assertBadRequest(() -> new ActivityRecordImageUploadPurposePolicy(clock).validateFileSize(0L));
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
