package com.detoxmate.upload.service;

import com.detoxmate.upload.config.StorageS3Properties;
import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3UploadService implements UploadService {

    private final S3Presigner s3Presigner;
    private final StorageS3Properties storageS3Properties;
    private final UploadContentTypePolicy uploadContentTypePolicy;
    private final UploadFileSizePolicy uploadFileSizePolicy;
    private final UploadObjectKeyFactory uploadObjectKeyFactory;

    @Override
    public PresignedUrlResponse issuePresignedUrl(Long userId, PresignedUrlRequest request) {
        String contentType = uploadContentTypePolicy.validate(request.contentType());
        uploadFileSizePolicy.validate(request.uploadPurpose(), request.fileSize());

        String objectKey = uploadObjectKeyFactory.create(userId, request.uploadPurpose(), request.fileName());
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
}
