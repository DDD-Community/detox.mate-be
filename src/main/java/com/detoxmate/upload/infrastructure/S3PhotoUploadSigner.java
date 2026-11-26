package com.detoxmate.upload.infrastructure;

import com.detoxmate.upload.config.StorageS3Properties;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.service.PhotoUploadSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class S3PhotoUploadSigner implements PhotoUploadSigner {

    private final S3Presigner s3Presigner;
    private final StorageS3Properties storageS3Properties;

    @Override
    public PresignedUrlResponse signUpload(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageS3Properties.bucketName())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(storageS3Properties.presignedUrlExpiresIn()))
                .putObjectRequest(putObjectRequest)
                .build();

        return new PresignedUrlResponse(
                s3Presigner.presignPutObject(presignRequest).url().toString(),
                objectKey,
                storageS3Properties.presignedUrlExpiresIn()
        );
    }
}
