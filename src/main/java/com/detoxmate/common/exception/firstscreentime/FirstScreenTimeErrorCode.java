package com.detoxmate.common.exception.firstscreentime;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FirstScreenTimeErrorCode implements ErrorCode {

    CAN_REGISTER_PARTICIPATE_CHALLENGE(HttpStatus.FORBIDDEN, "참여 중인 챌린지만 등록할 수 있습니다."),
    FIRST_SCREEN_TIME_ALREADY_REGISTER(HttpStatus.CONFLICT, "첫 스크린타임은 이미 등록되었습니다."),
    CAN_NOT_FIND_PARTICIPATE_INFORMATION(HttpStatus.NOT_FOUND, "참여 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    FirstScreenTimeErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
