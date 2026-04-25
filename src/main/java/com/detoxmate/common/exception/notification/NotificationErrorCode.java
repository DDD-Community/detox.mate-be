package com.detoxmate.common.exception.notification;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode implements ErrorCode {

    INVALID_TYPE_CODE(HttpStatus.BAD_REQUEST, "잘못된 알림 타입코드입니다."),
    NOTIFICATION_TYPE_REQUIRED(HttpStatus.BAD_REQUEST,"알림 타입 코드는 필수값입니다."),
    NOTIFICATION_TITLE_REQUIRED(HttpStatus.BAD_REQUEST,"알림 제목은 필수값입니다."),
    NOTIFICATION_MESSAGE_TEMPLATE_REQUIRED(HttpStatus.BAD_REQUEST,"알림 메시지는 필수값입니다."),
    NOTIFICATION_NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "알림은 닉네임이 필수입니다."),
    NOTIFICATION_HISTORY_NOTIFICATION_REQUIRED(HttpStatus.BAD_REQUEST,"알림 히스토리에는 알림값이 필수입니다."),
    NOTIFICATION_HISTORY_USER_ID_REQUIRED(HttpStatus.BAD_REQUEST,"알림 히스토리에는 사용자 ID가 필수입니다."),

    INVALID_TITLE(HttpStatus.BAD_REQUEST, "잘못된 알림 제목입니다."),
    NOTIFICATION_TITLE_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "알림 제목은 50자를 초과했습니다."),
    NOTIFICATION_MESSAGE_TEMPLATE_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "알림 메시지는 255자를 초과했습니다."),

    ALREADY_READ(HttpStatus.BAD_REQUEST, "이미 읽은 알림입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_CONTEXT_INVALID_PAIRS(HttpStatus.INTERNAL_SERVER_ERROR, "알림 컨텍스트 변수는 키-값 쌍으로 전달되어야 합니다.");

    private final HttpStatus httpStatus;
    private final String message;

    NotificationErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
