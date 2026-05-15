package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class UploadObjectKeyValidatorTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-04-28T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final UploadObjectKeyValidator uploadObjectKeyValidator =
            new UploadObjectKeyValidator(uploadPurposePolicies());

    @Test
    void 생성된_profile_image_object_key는_같은_사용자와_목적에서_유효하다() {
        String objectKey = uploadPurposePolicy(UploadPurpose.PROFILE_IMAGE).createObjectKey(1L, "profile.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.PROFILE_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    void 생성된_activity_record_image_object_key는_같은_사용자와_목적에서_유효하다() {
        String objectKey = uploadPurposePolicy(UploadPurpose.ACTIVITY_RECORD_IMAGE).createObjectKey(1L, "walk.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.ACTIVITY_RECORD_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    void 생성된_screen_time_ocr_report_image_object_key는_같은_사용자와_목적에서_유효하다() {
        String objectKey = uploadPurposePolicy(UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE)
                .createObjectKey(1L, "screen.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    void 생성된_profile_image_object_key가_다른_사용자이면_유효하지_않다() {
        String objectKey = uploadPurposePolicy(UploadPurpose.PROFILE_IMAGE).createObjectKey(1L, "profile.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                2L,
                UploadPurpose.PROFILE_IMAGE,
                objectKey
        )).isFalse();
    }

    @Test
    void 생성된_profile_image_object_key가_다른_목적이면_유효하지_않다() {
        String objectKey = uploadPurposePolicy(UploadPurpose.PROFILE_IMAGE).createObjectKey(1L, "profile.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.ACTIVITY_RECORD_IMAGE,
                objectKey
        )).isFalse();
    }

    @Test
    void object_key가_비어있으면_유효하지_않다() {
        assertThat(uploadObjectKeyValidator.isOwnedBy(1L, UploadPurpose.PROFILE_IMAGE, " ")).isFalse();
    }

    private java.util.List<UploadPurposePolicy> uploadPurposePolicies() {
        return java.util.List.of(
                new ProfileImageUploadPurposePolicy(),
                new ActivityRecordImageUploadPurposePolicy(clock),
                new ScreenTimeOcrReportImageUploadPurposePolicy(clock)
        );
    }

    private UploadPurposePolicy uploadPurposePolicy(UploadPurpose uploadPurpose) {
        return uploadPurposePolicies().stream()
                .filter(policy -> policy.purpose() == uploadPurpose)
                .findFirst()
                .orElseThrow();
    }
}
