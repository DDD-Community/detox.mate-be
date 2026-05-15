package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public interface UploadPurposePolicy {

    String INVALID_FILE_SIZE_MESSAGE = "fileSize 는 양수여야 합니다.";
    String FILE_TOO_LARGE_MESSAGE = "업로드 파일 크기가 허용 범위를 초과했습니다.";

    UploadPurpose purpose();

    long maxFileSize();

    String objectKeyPrefixForUser(Long userId);

    String objectKeyDirectory(Long userId);

    default void validateFileSize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_FILE_SIZE_MESSAGE);
        }

        if (fileSize > maxFileSize()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, FILE_TOO_LARGE_MESSAGE);
        }
    }

    default String createObjectKey(Long userId, String fileName) {
        String sanitizedFileName = sanitize(fileName);
        String uuid = UUID.randomUUID().toString();
        return objectKeyDirectory(userId) + uuid + "-" + sanitizedFileName;
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
