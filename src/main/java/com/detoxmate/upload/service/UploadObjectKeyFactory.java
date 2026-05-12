package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Component
class UploadObjectKeyFactory {

    private final Clock clock;

    UploadObjectKeyFactory(Clock clock) {
        this.clock = clock;
    }

    String create(Long userId, UploadPurpose uploadPurpose, String fileName) {
        String sanitizedFileName = sanitize(fileName);
        String uuid = UUID.randomUUID().toString();

        return switch (uploadPurpose) {
            case ACTIVITY_RECORD_IMAGE -> activityRecordImageKey(userId, sanitizedFileName, uuid);
            case PROFILE_IMAGE -> profileImageKey(userId, sanitizedFileName, uuid);
            case SCREEN_TIME_OCR_REPORT_IMAGE -> screenTimeOcrReportImageKey(userId, sanitizedFileName, uuid);
        };
    }

    private String activityRecordImageKey(Long userId, String sanitizedFileName, String uuid) {
        LocalDate now = LocalDate.now(clock);
        return UploadObjectKeyPath.activityRecordImageDirectory(userId, now) + uuid + "-" + sanitizedFileName;
    }

    private String profileImageKey(Long userId, String sanitizedFileName, String uuid) {
        return UploadObjectKeyPath.objectKeyPrefixForUser(userId, UploadPurpose.PROFILE_IMAGE) + uuid + "-" + sanitizedFileName;
    }

    private String screenTimeOcrReportImageKey(Long userId, String sanitizedFileName, String uuid) {
        LocalDate now = LocalDate.now(clock);
        return UploadObjectKeyPath.screenTimeOcrReportImageDirectory(userId, now) + uuid + "-" + sanitizedFileName;
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
