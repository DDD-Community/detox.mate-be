package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.dto.UploadPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhotoUploadService implements UploadService {

    private static final String UNSUPPORTED_UPLOAD_PURPOSE_MESSAGE = "지원하지 않는 업로드 목적입니다.";

    private final PhotoUploadSigner signer;
    private final UploadContentTypePolicy uploadContentTypePolicy;
    private final List<UploadPurposePolicy> uploadPurposePolicies;

    @Override
    public PresignedUrlResponse issuePresignedUrl(Long userId, PresignedUrlRequest request) {
        String contentType = uploadContentTypePolicy.validate(request.contentType());
        UploadPurposePolicy uploadPurposePolicy = uploadPurposePolicy(request.uploadPurpose());
        uploadPurposePolicy.validateFileSize(request.fileSize());

        String objectKey = uploadPurposePolicy.createObjectKey(userId, request.fileName());
        return signer.signUpload(objectKey, contentType);
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
