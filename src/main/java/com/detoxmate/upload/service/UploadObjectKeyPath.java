package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;

import java.time.LocalDate;

final class UploadObjectKeyPath {

    private static final String ACTIVITY_RECORD_IMAGE_DIRECTORY = "activity-records";
    private static final String PROFILE_IMAGE_DIRECTORY = "profile-images";

    private UploadObjectKeyPath() {
    }

    static String objectKeyPrefixForUser(Long userId, UploadPurpose uploadPurpose) {
        return switch (uploadPurpose) {
            case ACTIVITY_RECORD_IMAGE -> ACTIVITY_RECORD_IMAGE_DIRECTORY + "/" + userId + "/";
            case PROFILE_IMAGE -> PROFILE_IMAGE_DIRECTORY + "/" + userId + "/";
        };
    }

    static String activityRecordImageDirectory(Long userId, LocalDate date) {
        return objectKeyPrefixForUser(userId, UploadPurpose.ACTIVITY_RECORD_IMAGE)
                + date.getYear() + "/"
                + String.format("%02d", date.getMonthValue()) + "/";
    }
}
