package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.dto.UploadPurpose;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class MockUploadService implements UploadService {

    private static final Map<UploadPurpose, String> PREFIXES = Map.of(
            UploadPurpose.ACTIVITY_RECORD_IMAGE, "activity-records",
            UploadPurpose.PROFILE_IMAGE, "profile-images"
    );

    @Override
    public PresignedUrlResponse issuePresignedUrl(Long userId, PresignedUrlRequest request) {
        String sanitizedFileName = sanitize(request.fileName());
        String objectKey = PREFIXES.get(request.uploadPurpose()) + "/" + UUID.randomUUID() + "-" + sanitizedFileName;
        String uploadUrl = "https://detoxmate-bucket.s3.ap-northeast-2.amazonaws.com/" + objectKey + "?signature=mock-signature";
        return new PresignedUrlResponse(uploadUrl, objectKey, 600);
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
