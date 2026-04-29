package com.detoxmate.upload.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Component
class UploadContentTypePolicy {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private static final String UNSUPPORTED_CONTENT_TYPE_MESSAGE = "지원하지 않는 contentType 입니다.";

    String validate(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNSUPPORTED_CONTENT_TYPE_MESSAGE);
        }

        return contentType;
    }
}
