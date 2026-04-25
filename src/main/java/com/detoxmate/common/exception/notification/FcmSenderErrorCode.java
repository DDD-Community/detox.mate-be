package com.detoxmate.common.exception.notification;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FcmSenderErrorCode implements ErrorCode {
    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FCM 메시지 전송에 실패했습니다."),
    FCM_INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 FCM 토큰입니다."),
    FCM_TOKEN_UNREGISTERED(HttpStatus.NOT_FOUND, "등록 해제된 FCM 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    FcmSenderErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
