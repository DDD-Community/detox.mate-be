package com.detoxmate.common.exception.feed;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FeedErrorCode implements ErrorCode {

    ACTIVITY_RECORD_CHALLENGE_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "인증글 챌린지 상태를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    FeedErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
