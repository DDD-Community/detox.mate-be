package com.detoxmate.common.exception.poke;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PokeErrorCode implements ErrorCode {

    POKE_GROUP_CHALLENGE_REQUIRED(HttpStatus.BAD_REQUEST, "그룹 챌린지는 필수입니다."),
    POKE_ACTIVITY_RECORD_REQUIRED(HttpStatus.BAD_REQUEST, "인증글은 필수입니다."),
    POKE_SENDER_REQUIRED(HttpStatus.BAD_REQUEST, "찌르기를 보낸 사용자는 필수입니다."),
    POKE_RECEIVER_REQUIRED(HttpStatus.BAD_REQUEST, "찌르기를 받은 사용자는 필수입니다."),
    POKE_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "찌른 날짜는 필수입니다."),
    POKE_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신은 찌를 수 없습니다."),
    POKE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 찌른 사용자입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    PokeErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

}
