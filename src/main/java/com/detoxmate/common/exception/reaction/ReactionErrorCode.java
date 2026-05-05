package com.detoxmate.common.exception.reaction;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReactionErrorCode implements ErrorCode {

    REACTION_CHALLENGE_RECORD_REQUIRED(HttpStatus.BAD_REQUEST, "챌린지 기록 ID는 필수입니다."),
    REACTION_USER_REQUIRED(HttpStatus.BAD_REQUEST, "리액션 작성자 ID는 필수입니다."),
    REACTION_BODY_REQUIRED(HttpStatus.BAD_REQUEST, "리액션 종류는 필수입니다."),
    REACTION_NOT_ALLOWED_BEFORE_RECORD(HttpStatus.BAD_REQUEST, "인증 전 기록에는 리액션을 남길 수 없습니다."),
    REACTION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 같은 리액션을 남겼습니다."),
    REACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "리액션을 찾을 수 없습니다."),
    REACTION_CHALLENGE_RECORD_MISMATCH(HttpStatus.BAD_REQUEST, "요청한 챌린지 기록에 속한 리액션이 아닙니다."),
    REACTION_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "리액션을 삭제할 권한이 없습니다."),
    REACTION_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 리액션입니다.");


    private final HttpStatus httpStatus;
    private final String message;

    ReactionErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

}
