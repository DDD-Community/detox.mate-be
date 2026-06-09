package com.detoxmate.common.error;

import com.detoxmate.common.logging.ApiLogWriter;

public final class GlobalExceptionHandlerTestFixture {

    private GlobalExceptionHandlerTestFixture() {
    }

    public static GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler(new ApiLogWriter());
    }
}
