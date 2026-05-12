package com.detoxmate.notification.util;

import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.domain.CommentStatus;
import com.detoxmate.comment.repository.CommentRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.comment.CommentErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class NotificationCommentReaderTest {

    @Autowired
    private CommentRepository commentRepository;

    private NotificationCommentReader reader;

    @BeforeEach
    void setUp() {
        reader = new NotificationCommentReader(commentRepository);
    }

    @Test
    @DisplayName("commentId로 댓글 본문을 조회한다")
    void findCommentBody_returnsCommentBody() {
        // given
        Comment comment = commentRepository.save(
                Comment.create(1L, 10L, "댓글 내용", CommentStatus.AFTER_RECORD)
        );

        // when
        String commentBody = reader.findCommentBody(comment.getId());

        // then
        assertThat(commentBody).isEqualTo("댓글 내용");
    }

    @Test
    @DisplayName("commentId에 해당하는 댓글이 없으면 COMMENT_NOT_FOUND 예외를 던진다")
    void findCommentBody_throwsWhenCommentDoesNotExist() {
        // when & then
        assertThatThrownBy(() -> reader.findCommentBody(999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
    }
}
