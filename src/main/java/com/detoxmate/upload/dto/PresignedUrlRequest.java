package com.detoxmate.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUrlRequest(
        @NotBlank
        String fileName,

        @NotBlank
        String contentType,

        @NotNull
        UploadPurpose uploadPurpose
) {
}
