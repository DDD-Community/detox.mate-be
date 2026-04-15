package com.detoxmate.common.exception.notification;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode implements ErrorCode {

    INVALID_TYPE_CODE(HttpStatus.BAD_REQUEST, "알림 타입 코드는 필수값입니다."),
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "알림 제목은 필수값입니다."),
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "알림 제목은 50자를 초과할 수 없습니다."),
    ALREADY_READ(HttpStatus.BAD_REQUEST, "이미 읽은 알림입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    NotificationErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
