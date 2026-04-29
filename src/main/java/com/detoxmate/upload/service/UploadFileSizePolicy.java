package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
class UploadFileSizePolicy {

    private static final long MB = 1024L * 1024L;
    private static final String INVALID_FILE_SIZE_MESSAGE = "fileSize 는 양수여야 합니다.";
    private static final String FILE_TOO_LARGE_MESSAGE = "업로드 파일 크기가 허용 범위를 초과했습니다.";

    private static final Map<UploadPurpose, Long> MAX_FILE_SIZE_BY_PURPOSE = Map.of(
            UploadPurpose.PROFILE_IMAGE, 5L * MB,
            UploadPurpose.ACTIVITY_RECORD_IMAGE, 10L * MB
    );

    void validate(UploadPurpose uploadPurpose, Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_FILE_SIZE_MESSAGE);
        }

        long maxFileSize = MAX_FILE_SIZE_BY_PURPOSE.get(uploadPurpose);

        if (fileSize > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, FILE_TOO_LARGE_MESSAGE);
        }
    }
}
