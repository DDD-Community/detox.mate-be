package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.PresignedUrlResponse;

public interface PhotoUploadSigner {

    PresignedUrlResponse signUpload(String objectKey, String contentType);
}
