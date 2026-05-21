package com.detoxmate.common.exception.feed;

import com.detoxmate.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FeedErrorCode implements ErrorCode {

    FEED_GROUP_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "피드의 그룹 챌린지를 찾을 수 없습니다."),
    FEED_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "피드의 그룹 정보를 찾을 수 없습니다."),
    FEED_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "피드 참여자 정보를 찾을 수 없습니다."),
    FEED_ACTIVITY_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "피드의 인증 기록을 찾을 수 없습니다."),
    FEED_ACCESS_DENIED(HttpStatus.FORBIDDEN,"참여 중인 챌린지의 피드만 조회할 수 있습니다."),
    FEED_HISTORY_DATE_MUST_BE_PAST(HttpStatus.BAD_REQUEST, "히스토리 피드는 오늘 이전 날짜만 조회할 수 있습니다."),
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),

    ACTIVITY_RECORD_CHALLENGE_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "인증글 챌린지 상태를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    FeedErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
