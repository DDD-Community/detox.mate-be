package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UploadObjectKeyValidator {

    private static final String UNSUPPORTED_UPLOAD_PURPOSE_MESSAGE = "지원하지 않는 업로드 목적입니다.";

    private final List<UploadPurposePolicy> uploadPurposePolicies;

    public boolean isOwnedBy(Long userId, UploadPurpose uploadPurpose, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }

        return objectKey.startsWith(uploadPurposePolicy(uploadPurpose).objectKeyPrefixForUser(userId));
    }

    private UploadPurposePolicy uploadPurposePolicy(UploadPurpose uploadPurpose) {
        return uploadPurposePolicies.stream()
                .filter(policy -> policy.purpose() == uploadPurpose)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        UNSUPPORTED_UPLOAD_PURPOSE_MESSAGE
                ));
    }
}
