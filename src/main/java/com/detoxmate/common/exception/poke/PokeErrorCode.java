package com.detoxmate.common.exception.poke;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PokeErrorCode implements ErrorCode {

    POKE_CHALLENGE_RECORD_REQUIRED(HttpStatus.BAD_REQUEST, "챌린지 기록 ID는 필수입니다."),
    POKE_SENDER_REQUIRED(HttpStatus.BAD_REQUEST, "콕 찌르기 보낸 사람 ID는 필수입니다."),
    POKE_RECEIVER_REQUIRED(HttpStatus.BAD_REQUEST, "콕 찌르기 받은 사람 ID는 필수입니다."),
    POKE_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "콕 찌르기 날짜는 필수입니다."),
    POKE_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신을 콕 찌를 수 없습니다."),
    POKE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 콕 찌른 사용자입니다."),
    POKE_NOT_ALLOWED_AFTER_RECORD(HttpStatus.BAD_REQUEST, "인증 후 기록에는 콕 찌르기를 할 수 없습니다."),
    POKE_ONLY_TODAY_ALLOWED(HttpStatus.BAD_REQUEST, "오늘 기록에만 콕 찌르기를 할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    PokeErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

}
