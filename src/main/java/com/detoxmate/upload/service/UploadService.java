package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;

public interface UploadService {

    PresignedUrlResponse issuePresignedUrl(Long userId, PresignedUrlRequest request);
}
