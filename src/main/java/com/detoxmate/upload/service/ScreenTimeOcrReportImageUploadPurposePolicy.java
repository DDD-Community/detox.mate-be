package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class ScreenTimeOcrReportImageUploadPurposePolicy implements UploadPurposePolicy {

    private static final long MB = 1024L * 1024L;
    private static final String SCREEN_TIME_OCR_REPORT_IMAGE_DIRECTORY = "screen-time-ocr-reports";

    private final Clock clock;

    public ScreenTimeOcrReportImageUploadPurposePolicy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UploadPurpose purpose() {
        return UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE;
    }

    @Override
    public long maxFileSize() {
        return 10L * MB;
    }

    @Override
    public String objectKeyPrefixForUser(Long userId) {
        return SCREEN_TIME_OCR_REPORT_IMAGE_DIRECTORY + "/" + userId + "/";
    }

    @Override
    public String objectKeyDirectory(Long userId) {
        LocalDate now = LocalDate.now(clock);
        return objectKeyPrefixForUser(userId)
                + now.getYear() + "/"
                + String.format("%02d", now.getMonthValue()) + "/";
    }
}
