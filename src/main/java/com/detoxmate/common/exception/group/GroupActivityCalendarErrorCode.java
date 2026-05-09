package com.detoxmate.common.exception.group;

import com.detoxmate.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GroupActivityCalendarErrorCode implements ErrorCode {
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."),
    GROUP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "내가 속한 그룹만 조회할 수 있습니다."),
    GROUP_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹 챌린지를 찾을 수 없습니다."),
    GROUP_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹 멤버를 찾을 수 없습니다."),
    ACTIVITY_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "활동 인증 기록을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    GroupActivityCalendarErrorCode(HttpStatus httpStatus, String message) {
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
