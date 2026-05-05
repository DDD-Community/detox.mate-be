package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.springframework.stereotype.Component;

@Component
public class UploadObjectKeyValidator {

    public boolean isOwnedBy(Long userId, UploadPurpose uploadPurpose, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }

        return objectKey.startsWith(UploadObjectKeyPath.userPrefix(userId, uploadPurpose));
    }
}
