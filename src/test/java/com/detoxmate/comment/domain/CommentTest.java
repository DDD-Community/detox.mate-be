package com.detoxmate.comment.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.comment.CommentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class CommentTest {

    private static final Long CHALLENGE_RECORD_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String VALID_BODY = "오늘 인증 화이팅!";

    @Test
    @DisplayName("댓글을 생성하면 챌린지 기록, 작성자, 본문, 댓글 상태가 저장된다")
    void createComment_savesValues() {
        //given & when
        Comment comment = Comment.create(CHALLENGE_RECORD_ID, USER_ID, VALID_BODY, CommentStatus.BEFORE_RECORD);

        //then
        assertThat(comment.getChallengeRecordId()).isEqualTo(CHALLENGE_RECORD_ID);
        assertThat(comment.getUserId()).isEqualTo(USER_ID);
        assertThat(comment.getCommentBody()).isEqualTo(VALID_BODY);
        assertThat(comment.getCommentStatus()).isEqualTo(CommentStatus.BEFORE_RECORD);
    }

    @Test
    @DisplayName("댓글을 생성하면 생성 시간이 설정된다")
    void createComment_initializesStatus() {
        //given & when
        Comment comment = Comment.create(CHALLENGE_RECORD_ID, USER_ID, VALID_BODY, CommentStatus.AFTER_RECORD);

        //then
        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("챌린지 기록이 없으면 댓글 생성이 실패한다")
    void createComment_failsWhenChallengeRecordIsNull() {
        //then
        assertThatThrownBy(() -> Comment.create(null, USER_ID, VALID_BODY, CommentStatus.BEFORE_RECORD))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_CHALLENGE_RECORD_REQUIRED);
    }

    @Test
    @DisplayName("본문이 null이면 댓글 생성이 실패한다")
    void createComment_failsWhenBodyIsNull() {
        //then
        assertThatThrownBy(() -> Comment.create(CHALLENGE_RECORD_ID, USER_ID, null, CommentStatus.BEFORE_RECORD))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_REQUIRED);
    }

    @Test
    @DisplayName("본문이 빈 문자열이면 댓글 생성이 실패한다")
    void createComment_failsWhenBodyIsEmpty() {
        //then
        assertThatThrownBy(() -> Comment.create(CHALLENGE_RECORD_ID, USER_ID, "", CommentStatus.BEFORE_RECORD))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_REQUIRED);
    }

    @Test
    @DisplayName("본문이 공백만 있으면 댓글 생성이 실패한다")
    void createComment_failsWhenBodyIsBlank() {
        //then
        assertThatThrownBy(() -> Comment.create(CHALLENGE_RECORD_ID, USER_ID, "   ", CommentStatus.BEFORE_RECORD))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_REQUIRED);
    }

    @Test
    @DisplayName("본문이 1000자를 초과하면 댓글 생성이 실패한다")
    void createComment_failsWhenBodyExceedsMaxLength() {
        //given
        String tooLong = "가".repeat(1001);

        //when & then
        assertThatThrownBy(() -> Comment.create(CHALLENGE_RECORD_ID, USER_ID, tooLong, CommentStatus.BEFORE_RECORD))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_LENGTH_EXCEEDED);
    }

    @Test
    @DisplayName("본문이 정확히 1000자면 댓글이 생성된다")
    void createComment_succeedsWhenBodyIsExactlyMaxLength() {
        //given
        String exactlyMax = "가".repeat(1000);

        //when
        Comment comment = Comment.create(CHALLENGE_RECORD_ID, USER_ID, exactlyMax, CommentStatus.BEFORE_RECORD);

        //then
        assertThat(comment.getCommentBody()).isEqualTo(exactlyMax);
    }

    @Test
    @DisplayName("댓글 상태가 없으면 댓글 생성이 실패한다")
    void createComment_failsWhenCommentStatusIsNull() {
        //then
        assertThatThrownBy(() -> Comment.create(CHALLENGE_RECORD_ID, USER_ID, VALID_BODY, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_STATUS_REQUIRED);
    }
}
