package com.detoxmate.upload.service;

import com.detoxmate.upload.config.StorageS3Properties;
import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.dto.UploadPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class S3UploadService implements UploadService {

    private static final String UNSUPPORTED_UPLOAD_PURPOSE_MESSAGE = "지원하지 않는 업로드 목적입니다.";

    private final S3Presigner s3Presigner;
    private final StorageS3Properties storageS3Properties;
    private final UploadContentTypePolicy uploadContentTypePolicy;
    private final List<UploadPurposePolicy> uploadPurposePolicies;

    @Override
    public PresignedUrlResponse issuePresignedUrl(Long userId, PresignedUrlRequest request) {
        String contentType = uploadContentTypePolicy.validate(request.contentType());
        UploadPurposePolicy uploadPurposePolicy = uploadPurposePolicy(request.uploadPurpose());
        uploadPurposePolicy.validateFileSize(request.fileSize());

        String objectKey = uploadPurposePolicy.createObjectKey(userId, request.fileName());
        PresignedPutObjectRequest presignedRequest = presignPutObject(objectKey, contentType);

        return new PresignedUrlResponse(
                presignedRequest.url().toString(),
                objectKey,
                storageS3Properties.presignedUrlExpiresIn()
        );
    }

    private PresignedPutObjectRequest presignPutObject(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageS3Properties.bucketName())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(storageS3Properties.presignedUrlExpiresIn()))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest);
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
