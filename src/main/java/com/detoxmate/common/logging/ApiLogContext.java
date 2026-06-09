package com.detoxmate.common.logging;

public record ApiLogContext(
        String requestId,
        String userId,
        String method,
        String path
) {
}
