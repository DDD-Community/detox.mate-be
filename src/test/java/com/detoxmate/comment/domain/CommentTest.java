package com.detoxmate.comment.domain;

import com.detoxmate.common.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class CommentTest {

    private static final Long ANY_STAMP_ID = 100L;
    private static final Long ANY_CHALLENGE_ID = 10L;
    private static final Long ANY_USER_ID = 1L;
    private static final String VALID_BODY = "오늘 운동 화이팅!";

    @Test
    void 댓글을_생성하면_본문이_저장된다() {
        //given & when
        Comment comment = Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.getBody()).isEqualTo(VALID_BODY);
    }

    @Test
    void 댓글을_생성하면_작성자와_연관_ID가_저장된다() {
        //given & when
        Comment comment = Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.getActivityRecordId()).isEqualTo(ANY_STAMP_ID);
        assertThat(comment.getGroupChallengeId()).isEqualTo(ANY_CHALLENGE_ID);
        assertThat(comment.getUserId()).isEqualTo(ANY_USER_ID);
    }

    @Test
    void 댓글을_생성하면_isDeleted는_false이다() {
        //given & when
        Comment comment = Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.isDeleted()).isFalse();
    }

    @Test
    void 댓글을_생성하면_createdAt이_설정된다() {
        //given & when
        Comment comment = Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.getCreatedAt()).isNotNull();
    }

    @Test
    void 댓글을_생성하면_updatedAt은_null이다() {
        //given & when
        Comment comment = Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    void 본문이_null이면_댓글_생성이_실패한다() {
        //when & then
        assertThatThrownBy(() -> Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, null))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_BLANK);
    }

    @Test
    void 본문이_빈_문자열이면_댓글_생성이_실패한다() {
        //when & then
        assertThatThrownBy(() -> Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, ""))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_BLANK);
    }

    @Test
    void 본문이_공백만_있으면_댓글_생성이_실패한다() {
        //when & then
        assertThatThrownBy(() -> Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, "   "))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CommentErrorCode.COMMENT_BODY_BLANK);
    }

    @Test
    void 본문이_1000자를_초과하면_댓글_생성이_실패한다() {
        //given
        String tooLong = "가".repeat(1000);

        //when & then
        assertThatThrownBy(() -> Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, tooLong))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_TOO_LONG);
    }

    @Test
    void 본문이_정확히_1000자면_댓글이_생성된다() {
        //given
        String exactly255 = "가".repeat(1000);

        //when
        Comment comment = Comment.create(ANY_STAMP_ID, ANY_CHALLENGE_ID, ANY_USER_ID, exactly255);

        //then
        assertThat(comment.getBody()).isEqualTo(exactly255);
    }
}
