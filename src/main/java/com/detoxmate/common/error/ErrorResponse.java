package com.detoxmate.common.error;

public record ErrorResponse(
        String code,
        String message,
        int status
) {
}
