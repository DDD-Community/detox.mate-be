package com.detoxmate.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApiLogWriter {

    public void httpRequestBody(ApiLogContext context, String body) {
        log.info("event=http_request_body requestId={} userId={} method={} path={} body={}",
                context.requestId(), context.userId(), context.method(), context.path(), body);
    }

    public void httpResponseBody(ApiLogContext context, int status, String body) {
        log.info("event=http_response_body requestId={} userId={} method={} path={} status={} body={}",
                context.requestId(), context.userId(), context.method(), context.path(), status, body);
    }

    public void httpRequest(ApiLogContext context, String query, int status, long durationMs) {
        log.info("event=http_request requestId={} userId={} method={} path={} query={} status={} durationMs={}",
                context.requestId(), context.userId(), context.method(), context.path(), query, status, durationMs);
    }

    public void handledError(ApiLogContext context, int status, String errorCode) {
        log.info("event=api_handled_error requestId={} userId={} method={} path={} status={} errorCode={}",
                context.requestId(), context.userId(), context.method(), context.path(), status, errorCode);
    }

    public void apiError(ApiLogContext context, int status, Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();
        log.error("event=api_error requestId={} userId={} method={} path={} status={} exception={} fingerprint={}",
                context.requestId(),
                context.userId(),
                context.method(),
                context.path(),
                status,
                exceptionName,
                context.method() + ":" + context.path() + ":" + exceptionName,
                exception);
    }

    public void apiLoggingFailed(ApiLogContext context, RuntimeException exception) {
        log.warn("event=api_logging_failed requestId={} userId={} method={} path={}",
                context.requestId(), context.userId(), context.method(), context.path(), exception);
    }
}
