package com.detoxmate.common.error;

import com.detoxmate.common.logging.ApiLogContext;
import com.detoxmate.common.logging.ApiLogWriter;
import com.detoxmate.common.exception.CustomException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ApiLogWriter apiLogWriter;

    public GlobalExceptionHandler(ApiLogWriter apiLogWriter) {
        this.apiLogWriter = apiLogWriter;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception exception, HttpServletRequest request) {
        ErrorResponse errorResponse = toErrorResponse(HttpStatus.BAD_REQUEST);
        logHandledError(request, HttpStatus.BAD_REQUEST, errorResponse.code());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        ErrorResponse errorResponse = toErrorResponse(status);
        logHandledError(request, status, errorResponse.code());

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler({JwtException.class, NumberFormatException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(Exception exception, HttpServletRequest request) {
        ErrorResponse errorResponse = toErrorResponse(HttpStatus.UNAUTHORIZED);
        logHandledError(request, HttpStatus.UNAUTHORIZED, errorResponse.code());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse);
    }

    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ErrorResponse> handleServletException(ServletException exception, HttpServletRequest request) {
        Throwable cause = exception.getCause();

        if (cause instanceof JwtException || cause instanceof NumberFormatException || cause instanceof NoSuchElementException) {
            ErrorResponse errorResponse = toErrorResponse(HttpStatus.UNAUTHORIZED);
            logHandledError(request, HttpStatus.UNAUTHORIZED, errorResponse.code());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse);
        }

        ErrorResponse errorResponse = toErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        logApiError(request, HttpStatus.INTERNAL_SERVER_ERROR, exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception, HttpServletRequest request) {
        HttpStatus status = exception.getErrorCode().getHttpStatus();
        logHandledError(request, status, exception.getErrorCode().name());

        return ResponseEntity.status(status)
                .body(new ErrorResponse(
                        exception.getErrorCode().name(),
                        exception.getErrorCode().getMessage(),
                        status.value()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        ErrorResponse errorResponse = toErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        logApiError(request, HttpStatus.INTERNAL_SERVER_ERROR, exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    private ErrorResponse toErrorResponse(HttpStatus status) {
        if (status == HttpStatus.BAD_REQUEST) {
            return new ErrorResponse("INVALID_REQUEST", "Invalid request", status.value());
        }

        if (status == HttpStatus.UNAUTHORIZED) {
            return new ErrorResponse("UNAUTHORIZED", "Unauthorized", status.value());
        }

        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return new ErrorResponse("INTERNAL_SERVER_ERROR", "Internal server error", status.value());
        }

        return new ErrorResponse(status.name(), status.getReasonPhrase(), status.value());
    }

    private void logHandledError(HttpServletRequest request, HttpStatus status, String errorCode) {
        apiLogWriter.handledError(toApiLogContext(request), status.value(), errorCode);
    }

    private void logApiError(HttpServletRequest request, HttpStatus status, Exception exception) {
        apiLogWriter.apiError(toApiLogContext(request), status.value(), exception);
    }

    private ApiLogContext toApiLogContext(HttpServletRequest request) {
        return new ApiLogContext(
                mdcOrDefault("requestId", "unknown"),
                mdcOrDefault("userId", "anonymous"),
                request.getMethod(),
                request.getRequestURI()
        );
    }

    private String mdcOrDefault(String key, String defaultValue) {
        String value = MDC.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
