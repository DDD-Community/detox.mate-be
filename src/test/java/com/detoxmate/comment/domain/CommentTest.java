package com.detoxmate.comment.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.comment.CommentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class CommentTest {

    private static final Long ANY_ACTIVITY_RECORD_ID = 100L;
    private static final Long ANY_GROUP_CHALLENGE_ID = 10L;
    private static final Long ANY_USER_ID = 1L;
    private static final String VALID_BODY = "오늘 운동 화이팅!";

    @Test
    @DisplayName("댓글을 생성하면 본문이 저장된다.")
    void createComment_savesBody() {
        //given & when
        Comment comment = Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.getCommentBody()).isEqualTo(VALID_BODY);
    }

    @Test
    @DisplayName("댓글을 생성하면 작성자와 연관 ID가 저장된다")
    void createComment_savesAuthorAndRelatedIds() {
        //given & when
        Comment comment = Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.getActivityRecordId()).isEqualTo(ANY_ACTIVITY_RECORD_ID);
        assertThat(comment.getGroupChallengeId()).isEqualTo(ANY_GROUP_CHALLENGE_ID);
        assertThat(comment.getUserId()).isEqualTo(ANY_USER_ID);
    }

    @Test
    @DisplayName("댓글을 생성하면 createdAt이 설정된다")
    void createComment_setsCreatedAt() {
        //given & when
        Comment comment = Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글을 생성하면 updatedAt은 null이다")
    void createComment_updatedAtIsNull() {
        //given & when
        Comment comment = Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //then
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("본문이 null이면 댓글 생성이 실패한다")
    void createComment_failsWhenBodyIsNull() {
        //when & then
        assertThatThrownBy(() -> Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, null))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_REQUIRED);
    }

    @Test
    @DisplayName("본문이 빈 문자열이면 댓글 생성이 실패한다")
    void createComment_failsWhenBodyIsEmpty() {
        //when & then
        assertThatThrownBy(() -> Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, ""))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_REQUIRED);
    }

    @Test
    @DisplayName("본문이 공백만 있으면 댓글 생성이 실패한다")
    void createComment_failsWhenBodyIsBlank() {
        //when & then
        assertThatThrownBy(() -> Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "   "))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CommentErrorCode.COMMENT_BODY_REQUIRED);
    }

    @Test
    @DisplayName("본문이 1000자를 초과하면 댓글 생성이 실패한다")
    void createComment_failsWhenBodyExceedsMaxLength() {
        //given
        String tooLong = "가".repeat(10001);

        //when & then
        assertThatThrownBy(() -> Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, tooLong))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_BODY_LENGTH_EXCEEDED);
    }

    @Test
    @DisplayName("본문이 정확히 1000자면 댓글이 생성된다")
    void createComment_succeedsWhenBodyIsExactlyMaxLength() {
        //given
        String exactly255 = "가".repeat(1000);

        //when
        Comment comment = Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, exactly255);

        //then
        assertThat(comment.getCommentBody()).isEqualTo(exactly255);
    }
}
