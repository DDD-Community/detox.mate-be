package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("활동 기록 이미지 정책은 기존 object key 형식을 유지한다")
    void createObjectKey_keepsActivityRecordImageObjectKeyFormat() {
        // given
        UploadPurposePolicy policy = new ActivityRecordImageUploadPurposePolicy(clock);

        // when
        String objectKey = policy.createObjectKey(7L, "walk photo.png");

        // then
        assertThat(policy.purpose()).isEqualTo(UploadPurpose.ACTIVITY_RECORD_IMAGE);
        assertThat(objectKey)
                .startsWith("activity-records/7/2026/04/")
                .endsWith("-walk-photo.png");
    }

    @Test
    @DisplayName("프로필 이미지 정책은 기존 object key 형식을 유지한다")
    void createObjectKey_keepsProfileImageObjectKeyFormat() {
        // given
        UploadPurposePolicy policy = new ProfileImageUploadPurposePolicy();

        // when
        String objectKey = policy.createObjectKey(7L, "avatar.png");

        // then
        assertThat(policy.purpose()).isEqualTo(UploadPurpose.PROFILE_IMAGE);
        assertThat(objectKey)
                .startsWith("profile-images/7/")
                .endsWith("-avatar.png");
    }

    @Test
    @DisplayName("스크린 타임 OCR 리포트 이미지 정책은 기존 object key 형식을 유지한다")
    void createObjectKey_keepsScreenTimeOcrReportImageObjectKeyFormat() {
        // given
        UploadPurposePolicy policy = new ScreenTimeOcrReportImageUploadPurposePolicy(clock);

        // when
        String objectKey = policy.createObjectKey(7L, "screen time.png");

        // then
        assertThat(policy.purpose()).isEqualTo(UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE);
        assertThat(objectKey)
                .startsWith("screen-time-ocr-reports/7/2026/04/")
                .endsWith("-screen-time.png");
    }

    @Test
    @DisplayName("프로필 이미지 정책은 5MB까지 허용한다")
    void validateFileSize_allowsProfileImageUpTo5MB() {
        // when & then
        assertThatCode(() -> new ProfileImageUploadPurposePolicy().validateFileSize(5L * MB))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("활동 기록 이미지 정책은 10MB까지 허용한다")
    void validateFileSize_allowsActivityRecordImageUpTo10MB() {
        // when & then
        assertThatCode(() -> new ActivityRecordImageUploadPurposePolicy(clock).validateFileSize(10L * MB))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("스크린 타임 OCR 리포트 이미지 정책은 10MB까지 허용한다")
    void validateFileSize_allowsScreenTimeOcrReportImageUpTo10MB() {
        // when & then
        assertThatCode(() -> new ScreenTimeOcrReportImageUploadPurposePolicy(clock).validateFileSize(10L * MB))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("프로필 이미지가 5MB를 초과하면 400 에러를 반환한다")
    void validateFileSize_rejectsProfileImageOver5MB() {
        // when & then
        assertBadRequest(() -> new ProfileImageUploadPurposePolicy().validateFileSize(5L * MB + 1));
    }

    @Test
    @DisplayName("활동 기록 이미지가 10MB를 초과하면 400 에러를 반환한다")
    void validateFileSize_rejectsActivityRecordImageOver10MB() {
        // when & then
        assertBadRequest(() -> new ActivityRecordImageUploadPurposePolicy(clock).validateFileSize(10L * MB + 1));
    }

    @Test
    @DisplayName("스크린 타임 OCR 리포트 이미지가 10MB를 초과하면 400 에러를 반환한다")
    void validateFileSize_rejectsScreenTimeOcrReportImageOver10MB() {
        // when & then
        assertBadRequest(() -> new ScreenTimeOcrReportImageUploadPurposePolicy(clock).validateFileSize(10L * MB + 1));
    }

    @Test
    @DisplayName("파일 크기가 양수가 아니면 400 에러를 반환한다")
    void validateFileSize_rejectsNonPositiveFileSize() {
        // when & then
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
