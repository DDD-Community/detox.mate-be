package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;

import java.time.LocalDate;

final class UploadObjectKeyPath {

    private static final String ACTIVITY_RECORD_IMAGE_DIRECTORY = "activity-records";
    private static final String PROFILE_IMAGE_DIRECTORY = "profile-images";
    private static final String SCREEN_TIME_OCR_REPORT_IMAGE_DIRECTORY = "screen-time-ocr-reports";

    private UploadObjectKeyPath() {
    }

    static String objectKeyPrefixForUser(Long userId, UploadPurpose uploadPurpose) {
        return switch (uploadPurpose) {
            case ACTIVITY_RECORD_IMAGE -> ACTIVITY_RECORD_IMAGE_DIRECTORY + "/" + userId + "/";
            case PROFILE_IMAGE -> PROFILE_IMAGE_DIRECTORY + "/" + userId + "/";
            case SCREEN_TIME_OCR_REPORT_IMAGE -> SCREEN_TIME_OCR_REPORT_IMAGE_DIRECTORY + "/" + userId + "/";
        };
    }

    static String activityRecordImageDirectory(Long userId, LocalDate date) {
        return objectKeyPrefixForUser(userId, UploadPurpose.ACTIVITY_RECORD_IMAGE)
                + date.getYear() + "/"
                + String.format("%02d", date.getMonthValue()) + "/";
    }

    static String screenTimeOcrReportImageDirectory(Long userId, LocalDate date) {
        return objectKeyPrefixForUser(userId, UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE)
                + date.getYear() + "/"
                + String.format("%02d", date.getMonthValue()) + "/";
    }
}
