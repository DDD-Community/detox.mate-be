package com.detoxmate.common.exception.challengerecord;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChallengeRecordErrorCode implements ErrorCode {

    GROUP_CHALLENGE_REQUIRED(HttpStatus.BAD_REQUEST, "그룹 챌린지는 필수입니다."),
    GROUP_CHALLENGE_PARTICIPANT_REQUIRED(HttpStatus.BAD_REQUEST, "챌린지 참여자는 필수입니다."),
    RECORD_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "기록 날짜는 필수입니다."),
    ACTIVITY_RECORD_REQUIRED(HttpStatus.BAD_REQUEST, "인증 기록은 필수입니다."),
    CERTIFICATION_RESULT_REQUIRED(HttpStatus.BAD_REQUEST, "인증 결과는 필수입니다."),
    ACTIVITY_RECORD_PARTICIPANT_MISMATCH(HttpStatus.BAD_REQUEST, "챌린지 기록과 인증 기록의 참여자가 일치하지 않습니다."),
    CHALLENGE_RECORD_ALREADY_CERTIFIED(HttpStatus.BAD_REQUEST, "이미 인증된 챌린지 기록입니다."),
    CHALLENGE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "챌린지 기록을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ChallengeRecordErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
