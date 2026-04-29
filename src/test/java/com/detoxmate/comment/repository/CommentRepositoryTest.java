package com.detoxmate.comment.repository;

import com.detoxmate.comment.domain.Comment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    private static final Long ANY_ACTIVITY_RECORD_ID = 100L;
    private static final Long ANY_GROUP_CHALLENGE_ID = 10L;
    private static final Long ANY_USER_ID = 1L;
    private static final String VALID_BODY = "오늘 인증 화이팅";

    @Test
    @DisplayName("댓글을 저장하면 ID가 부여된다")
    void saveComment_assignsId() {
        //given
        Comment comment = Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        //when
        Comment saved = commentRepository.save(comment);

        //then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("댓글을 저장하면 모든 필드가 보존된다")
    void saveComment_preservesAllFields() {
        //given
        Comment comment = Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, VALID_BODY);

        Comment saved = commentRepository.save(comment);
        commentRepository.flush();

        assertThat(saved.getActivityRecordId()).isEqualTo(ANY_ACTIVITY_RECORD_ID);
        assertThat(saved.getGroupChallengeId()).isEqualTo(ANY_GROUP_CHALLENGE_ID);
        assertThat(saved.getUserId()).isEqualTo(ANY_USER_ID);
        assertThat(saved.getCommentBody()).isEqualTo(VALID_BODY);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("같은 activity record의 댓글만 조회된다")
    void findByActivityRecordId_returnsOnlyMatchingComments() {
        commentRepository.save(Comment.create(100L, 10L, 1L, "stamp100 댓글1"));
        commentRepository.save(Comment.create(100L, 10L, 2L, "stamp100 댓글2"));
        commentRepository.save(Comment.create(200L, 10L, 1L, "stamp200 댓글"));

        List<Comment> result = commentRepository.findAllByActivityRecordIdOrderByCreatedAtAsc(100L);

        assertThat(result).hasSize(2)
                .extracting(Comment::getCommentBody)
                .containsExactlyInAnyOrder("stamp100 댓글1", "stamp100 댓글2");
    }

    @Test
    @DisplayName("댓글이 없는 activity record는 빈 리스트가 반환된다")
    void findByActivityRecordId_returnsEmptyListWhenNoComments() {
        List<Comment> result = commentRepository.findAllByActivityRecordIdOrderByCreatedAtAsc(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("댓글은 생성 시각 오름차순으로 정렬된다")
    void findByActivityRecordId_returnsCommentsOrderedByCreatedAtAsc() throws InterruptedException {
        commentRepository.save(Comment.create(100L, 10L, 1L, "1번째"));
        Thread.sleep(2);
        commentRepository.save(Comment.create(100L, 10L, 1L, "2번째"));
        Thread.sleep(2);
        commentRepository.save(Comment.create(100L, 10L, 1L, "3번째"));

        List<Comment> result = commentRepository.findAllByActivityRecordIdOrderByCreatedAtAsc(100L);

        assertThat(result)
                .extracting(Comment::getCommentBody)
                .containsExactly("1번째", "2번째", "3번째");
    }

}
