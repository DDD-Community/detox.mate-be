package com.detoxmate.upload.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadContentTypePolicyTest {

    private final UploadContentTypePolicy uploadContentTypePolicy = new UploadContentTypePolicy();

    @Test
    void 허용된_이미지_contentType은_그대로_반환한다() {
        String contentType = uploadContentTypePolicy.validate("image/png");

        assertThat(contentType).isEqualTo("image/png");
    }

    @Test
    void 아이폰_원본_사진_contentType인_heic와_heif를_허용한다() {
        assertThat(uploadContentTypePolicy.validate("image/heic")).isEqualTo("image/heic");
        assertThat(uploadContentTypePolicy.validate("image/heif")).isEqualTo("image/heif");
    }

    @Test
    void 대소문자가_다른_contentType이면_400_에러를_반환한다() {
        assertThatThrownBy(() -> uploadContentTypePolicy.validate("IMAGE/PNG"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void 허용되지_않은_contentType이면_400_에러를_반환한다() {
        assertThatThrownBy(() -> uploadContentTypePolicy.validate("image/gif"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
