package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.springframework.stereotype.Component;

@Component
public class ProfileImageUploadPurposePolicy implements UploadPurposePolicy {

    private static final long MB = 1024L * 1024L;
    private static final String PROFILE_IMAGE_DIRECTORY = "profile-images";

    @Override
    public UploadPurpose purpose() {
        return UploadPurpose.PROFILE_IMAGE;
    }

    @Override
    public long maxFileSize() {
        return 5L * MB;
    }

    @Override
    public String objectKeyPrefixForUser(Long userId) {
        return PROFILE_IMAGE_DIRECTORY + "/" + userId + "/";
    }

    @Override
    public String objectKeyDirectory(Long userId) {
        return objectKeyPrefixForUser(userId);
    }
}
