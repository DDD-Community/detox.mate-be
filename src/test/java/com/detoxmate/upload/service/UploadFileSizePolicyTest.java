package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadFileSizePolicyTest {

    private static final long MB = 1024L * 1024L;

    private final UploadFileSizePolicy uploadFileSizePolicy = new UploadFileSizePolicy();

    @Test
    void profile_image는_5MB까지_허용한다() {
        assertThatCode(() -> uploadFileSizePolicy.validate(UploadPurpose.PROFILE_IMAGE, 5L * MB))
                .doesNotThrowAnyException();
    }

    @Test
    void activity_record_image는_10MB까지_허용한다() {
        assertThatCode(() -> uploadFileSizePolicy.validate(UploadPurpose.ACTIVITY_RECORD_IMAGE, 10L * MB))
                .doesNotThrowAnyException();
    }

    @Test
    void profile_image가_5MB를_초과하면_400_에러를_반환한다() {
        assertThatThrownBy(() -> uploadFileSizePolicy.validate(UploadPurpose.PROFILE_IMAGE, 5L * MB + 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void activity_record_image가_10MB를_초과하면_400_에러를_반환한다() {
        assertThatThrownBy(() -> uploadFileSizePolicy.validate(UploadPurpose.ACTIVITY_RECORD_IMAGE, 10L * MB + 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void fileSize가_양수가_아니면_400_에러를_반환한다() {
        assertThatThrownBy(() -> uploadFileSizePolicy.validate(UploadPurpose.ACTIVITY_RECORD_IMAGE, 0L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
