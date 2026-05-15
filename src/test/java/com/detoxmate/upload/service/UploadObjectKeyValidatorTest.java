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
    private final UploadObjectKeyValidator uploadObjectKeyValidator = new UploadObjectKeyValidator();
    private final UploadObjectKeyFactory uploadObjectKeyFactory = new UploadObjectKeyFactory(clock);

    @Test
    void 생성된_profile_image_object_key는_같은_사용자와_목적에서_유효하다() {
        String objectKey = uploadObjectKeyFactory.create(1L, UploadPurpose.PROFILE_IMAGE, "profile.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.PROFILE_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    void 생성된_activity_record_image_object_key는_같은_사용자와_목적에서_유효하다() {
        String objectKey = uploadObjectKeyFactory.create(1L, UploadPurpose.ACTIVITY_RECORD_IMAGE, "walk.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.ACTIVITY_RECORD_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    void 생성된_screen_time_ocr_report_image_object_key는_같은_사용자와_목적에서_유효하다() {
        String objectKey = uploadObjectKeyFactory.create(1L, UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE, "screen.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    void 생성된_profile_image_object_key가_다른_사용자이면_유효하지_않다() {
        String objectKey = uploadObjectKeyFactory.create(1L, UploadPurpose.PROFILE_IMAGE, "profile.png");

        assertThat(uploadObjectKeyValidator.isOwnedBy(
                2L,
                UploadPurpose.PROFILE_IMAGE,
                objectKey
        )).isFalse();
    }

    @Test
    void 생성된_profile_image_object_key가_다른_목적이면_유효하지_않다() {
        String objectKey = uploadObjectKeyFactory.create(1L, UploadPurpose.PROFILE_IMAGE, "profile.png");

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
}
