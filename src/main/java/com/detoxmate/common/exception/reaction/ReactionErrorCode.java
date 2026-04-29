package com.detoxmate.common.exception.reaction;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReactionErrorCode implements ErrorCode {

    REACTION_BODY_REQUIRED(HttpStatus.BAD_REQUEST, "리액션 본문은 필수값입니다."),
    REACTION_ALREADY_DELETED(HttpStatus.BAD_REQUEST,"작성자만 리액션을 삭제할 수 있습니다."),
    REACTION_DELETE_FORBIDDEN(HttpStatus.BAD_REQUEST,"이미 삭제된 리액션입니다.");


    private final HttpStatus httpStatus;
    private final String message;

    ReactionErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

}
