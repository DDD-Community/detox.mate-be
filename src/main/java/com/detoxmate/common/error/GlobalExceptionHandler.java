package com.detoxmate.common.error;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(toErrorResponse(status));
    }

    @ExceptionHandler({JwtException.class, IllegalArgumentException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(Exception exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(toErrorResponse(HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ErrorResponse> handleServletException(ServletException exception) {
        Throwable cause = exception.getCause();

        if (cause instanceof JwtException || cause instanceof NoSuchElementException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(toErrorResponse(HttpStatus.UNAUTHORIZED));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(toErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(toErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR));
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
}
