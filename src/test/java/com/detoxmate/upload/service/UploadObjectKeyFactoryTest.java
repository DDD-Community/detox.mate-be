package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class UploadObjectKeyFactoryTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-04-28T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final UploadObjectKeyFactory uploadObjectKeyFactory = new UploadObjectKeyFactory(clock);

    @Test
    void activity_record_image용_objectKey를_생성한다() {
        String objectKey = uploadObjectKeyFactory.create(7L, UploadPurpose.ACTIVITY_RECORD_IMAGE, "walk photo.png");

        assertThat(objectKey)
                .startsWith("activity-records/7/2026/04/")
                .endsWith("-walk-photo.png");
    }

    @Test
    void profile_image용_objectKey를_생성한다() {
        String objectKey = uploadObjectKeyFactory.create(7L, UploadPurpose.PROFILE_IMAGE, "avatar.png");

        assertThat(objectKey)
                .startsWith("profile-images/7/")
                .endsWith("-avatar.png");
    }

    @Test
    void screen_time_ocr_report_image용_objectKey를_생성한다() {
        String objectKey = uploadObjectKeyFactory.create(
                7L,
                UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE,
                "screen time.png"
        );

        assertThat(objectKey)
                .startsWith("screen-time-ocr-reports/7/2026/04/")
                .endsWith("-screen-time.png");
    }
}
