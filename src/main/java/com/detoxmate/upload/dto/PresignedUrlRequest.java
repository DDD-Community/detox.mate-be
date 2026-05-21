package com.detoxmate.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignedUrlRequest(
        @NotBlank
        String fileName,

        @NotBlank
        String contentType,

        @NotNull
        @Positive
        Long fileSize,

        @NotNull
        UploadPurpose uploadPurpose
) {
}
