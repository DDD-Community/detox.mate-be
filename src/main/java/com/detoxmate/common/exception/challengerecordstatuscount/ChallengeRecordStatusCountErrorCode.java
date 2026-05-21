package com.detoxmate.common.exception.challengerecordstatuscount;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChallengeRecordStatusCountErrorCode implements ErrorCode {

    CHALLENGE_RECORD_REQUIRED(HttpStatus.BAD_REQUEST, "챌린지 기록은 필수입니다."),
    CHALLENGE_RECORD_STATUS_COUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "챌린지 기록 집계를 찾을 수 없습니다.");


    private final HttpStatus httpStatus;
    private final String message;

    ChallengeRecordStatusCountErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
