package com.detoxmate.common.exception.notification;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FcmTokenErrorCode implements ErrorCode {
    TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "FcmToken에서 Token은 필수값입니다."),
    USER_ID_REQUIRED(HttpStatus.BAD_REQUEST,"FcmToken에서 User Id는 필수값입니다."),
    PLATFORM_REQUIRED(HttpStatus.BAD_REQUEST,"FcmToken에서 Platform이 전달되어야합니다."),
    TOKEN_TOO_LONG(HttpStatus.BAD_REQUEST,"Token값이 4096자를 초과했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    FcmTokenErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
