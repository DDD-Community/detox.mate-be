package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class ActivityRecordImageUploadPurposePolicy implements UploadPurposePolicy {

    private static final long MB = 1024L * 1024L;
    private static final String ACTIVITY_RECORD_IMAGE_DIRECTORY = "activity-records";

    private final Clock clock;

    public ActivityRecordImageUploadPurposePolicy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UploadPurpose purpose() {
        return UploadPurpose.ACTIVITY_RECORD_IMAGE;
    }

    @Override
    public long maxFileSize() {
        return 10L * MB;
    }

    @Override
    public String objectKeyPrefixForUser(Long userId) {
        return ACTIVITY_RECORD_IMAGE_DIRECTORY + "/" + userId + "/";
    }

    @Override
    public String objectKeyDirectory(Long userId) {
        LocalDate now = LocalDate.now(clock);
        return objectKeyPrefixForUser(userId)
                + now.getYear() + "/"
                + String.format("%02d", now.getMonthValue()) + "/";
    }
}
