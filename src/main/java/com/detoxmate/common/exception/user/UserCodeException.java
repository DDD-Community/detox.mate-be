package com.detoxmate.common.exception.user;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserCodeException implements ErrorCode {
    INVALID_USER_CODE(HttpStatus.BAD_REQUEST,"올바른 사용자 코드가 아닙니다."),
    USER_CODE_REQUIRED(HttpStatus.BAD_REQUEST,"사용자 코드를 입력해 주세요.");


    private final HttpStatus httpStatus;
    private final String message;

    UserCodeException(HttpStatus httpStatus,String message){
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
