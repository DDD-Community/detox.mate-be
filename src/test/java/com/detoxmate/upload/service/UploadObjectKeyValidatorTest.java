package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UploadObjectKeyValidatorTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-04-28T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final List<UploadPurposePolicy> uploadPurposePolicies = List.of(
            new ProfileImageUploadPurposePolicy(),
            new ActivityRecordImageUploadPurposePolicy(clock),
            new ScreenTimeOcrReportImageUploadPurposePolicy(clock)
    );
    private final UploadObjectKeyValidator uploadObjectKeyValidator = new UploadObjectKeyValidator(uploadPurposePolicies);

    @Test
    @DisplayName("생성된 프로필 이미지 object key는 같은 사용자와 목적에서 유효하다")
    void isOwnedBy_returnsTrueForProfileImageOwnerAndPurpose() {
        // given
        String objectKey = uploadPurposePolicy(UploadPurpose.PROFILE_IMAGE).createObjectKey(1L, "profile.png");

        // when & then
        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.PROFILE_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    @DisplayName("생성된 활동 기록 이미지 object key는 같은 사용자와 목적에서 유효하다")
    void isOwnedBy_returnsTrueForActivityRecordImageOwnerAndPurpose() {
        // given
        String objectKey = uploadPurposePolicy(UploadPurpose.ACTIVITY_RECORD_IMAGE).createObjectKey(1L, "walk.png");

        // when & then
        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.ACTIVITY_RECORD_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    @DisplayName("생성된 스크린 타임 OCR 리포트 이미지 object key는 같은 사용자와 목적에서 유효하다")
    void isOwnedBy_returnsTrueForScreenTimeOcrReportImageOwnerAndPurpose() {
        // given
        String objectKey = uploadPurposePolicy(UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE)
                .createObjectKey(1L, "screen.png");

        // when & then
        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE,
                objectKey
        )).isTrue();
    }

    @Test
    @DisplayName("생성된 프로필 이미지 object key가 다른 사용자이면 유효하지 않다")
    void isOwnedBy_returnsFalseForDifferentUser() {
        // given
        String objectKey = uploadPurposePolicy(UploadPurpose.PROFILE_IMAGE).createObjectKey(1L, "profile.png");

        // when & then
        assertThat(uploadObjectKeyValidator.isOwnedBy(
                2L,
                UploadPurpose.PROFILE_IMAGE,
                objectKey
        )).isFalse();
    }

    @Test
    @DisplayName("생성된 프로필 이미지 object key가 다른 목적이면 유효하지 않다")
    void isOwnedBy_returnsFalseForDifferentPurpose() {
        // given
        String objectKey = uploadPurposePolicy(UploadPurpose.PROFILE_IMAGE).createObjectKey(1L, "profile.png");

        // when & then
        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.ACTIVITY_RECORD_IMAGE,
                objectKey
        )).isFalse();
    }

    @Test
    @DisplayName("object key가 비어있으면 유효하지 않다")
    void isOwnedBy_returnsFalseForBlankObjectKey() {
        // when & then
        assertThat(uploadObjectKeyValidator.isOwnedBy(1L, UploadPurpose.PROFILE_IMAGE, " ")).isFalse();
    }

    private UploadPurposePolicy uploadPurposePolicy(UploadPurpose uploadPurpose) {
        return uploadPurposePolicies.stream()
                .filter(policy -> policy.purpose() == uploadPurpose)
                .findFirst()
                .orElseThrow();
    }
}
