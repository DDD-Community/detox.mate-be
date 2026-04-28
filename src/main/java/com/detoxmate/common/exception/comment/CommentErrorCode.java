package com.detoxmate.common.exception.comment;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommentErrorCode implements ErrorCode {
    COMMENT_BODY_REQUIRED(HttpStatus.BAD_REQUEST, "댓글은 댓글메시지가 필수입니다."),
    COMMENT_BODY_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST,"댓글이 1000자를 초과했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    CommentErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
