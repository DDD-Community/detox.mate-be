package com.detoxmate.comment.repository;

import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.domain.CommentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    private static final Long CHALLENGE_RECORD_ID = 100L;
    private static final Long OTHER_CHALLENGE_RECORD_ID = 200L;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Test
    @DisplayName("댓글을 저장하면 ID가 부여된다")
    void saveComment_assignsId() {
        // given
        Comment comment = Comment.create(CHALLENGE_RECORD_ID, USER_ID, "댓글", CommentStatus.BEFORE_RECORD);

        // when
        Comment saved = commentRepository.save(comment);

        // then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("챌린지 기록과 댓글 상태가 같은 댓글만 ID 오름차순으로 조회된다")
    void findActiveByChallengeRecord_returnsOnlyMatchingStatusOrderedByIdAsc() {
        // given
        Comment c1 = commentRepository.save(Comment.create(CHALLENGE_RECORD_ID, USER_ID, "인증 전 댓글1", CommentStatus.BEFORE_RECORD));
        commentRepository.save(Comment.create(CHALLENGE_RECORD_ID, OTHER_USER_ID, "인증 후 댓글", CommentStatus.AFTER_RECORD));
        commentRepository.save(Comment.create(OTHER_CHALLENGE_RECORD_ID, USER_ID, "다른 챌린지 기록 댓글", CommentStatus.BEFORE_RECORD));
        Comment c2 = commentRepository.save(Comment.create(CHALLENGE_RECORD_ID, OTHER_USER_ID, "인증 전 댓글2", CommentStatus.BEFORE_RECORD));

        // when
        List<Comment> result = commentRepository.findByChallengeRecord(
                CHALLENGE_RECORD_ID,
                CommentStatus.BEFORE_RECORD,
                Pageable.ofSize(20)
        );

        // then
        assertThat(result)
                .extracting(Comment::getId)
                .containsExactly(c1.getId(), c2.getId());
    }

    @Test
    @DisplayName("첫 페이지 조회 시 size만큼만 반환된다")
    void findActiveByChallengeRecord_limitsBySize() {
        // given
        for (int i=0; i<5; i++) {
            commentRepository.save(Comment.create(CHALLENGE_RECORD_ID, USER_ID, "댓글" + i, CommentStatus.BEFORE_RECORD));
        }

        // when
        List<Comment> result = commentRepository.findByChallengeRecord(
                CHALLENGE_RECORD_ID,
                CommentStatus.BEFORE_RECORD,
                Pageable.ofSize(3)
        );

        // then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("댓글이 없으면 빈 리스트를 반환한다")
    void findActiveByChallengeRecord_returnsEmptyListWhenNoComments() {
        // when
        List<Comment> result = commentRepository.findByChallengeRecord(
                CHALLENGE_RECORD_ID,
                CommentStatus.BEFORE_RECORD,
                Pageable.ofSize(20)
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("cursor 이후의 댓글만 ID 오름차순으로 반환된다")
    void findActiveByChallengeRecordAfterCursor_returnsOnlyCommentsAfterCursor() {
        // given
        Comment c1 = commentRepository.save(Comment.create(
                CHALLENGE_RECORD_ID,
                USER_ID,
                "댓글1",
                CommentStatus.BEFORE_RECORD
        ));
        Comment c2 = commentRepository.save(Comment.create(
                CHALLENGE_RECORD_ID,
                USER_ID,
                "댓글2",
                CommentStatus.BEFORE_RECORD
        ));
        Comment c3 = commentRepository.save(Comment.create(
                CHALLENGE_RECORD_ID,
                USER_ID,
                "댓글3",
                CommentStatus.BEFORE_RECORD
        ));

        // when
        List<Comment> result = commentRepository.findByChallengeRecordAfterCursor(
                CHALLENGE_RECORD_ID,
                CommentStatus.BEFORE_RECORD,
                c1.getId(),
                Pageable.ofSize(20)
        );

        // then
        assertThat(result)
                .extracting(Comment::getId)
                .containsExactly(c2.getId(), c3.getId());
    }

    @Test
    @DisplayName("cursor 이후 조회 시 size만큼만 반환된다")
    void findActiveByChallengeRecordAfterCursor_limitsBySize() {
        // given
        Comment c1 = commentRepository.save(Comment.create(
                CHALLENGE_RECORD_ID,
                USER_ID,
                "기준 댓글",
                CommentStatus.BEFORE_RECORD
        ));

        for (int i=0; i<5; i++) {
            commentRepository.save(Comment.create(
                    CHALLENGE_RECORD_ID,
                    USER_ID,
                    "댓글" + i,
                    CommentStatus.BEFORE_RECORD
            ));
        }

        // when
        List<Comment> result = commentRepository.findByChallengeRecordAfterCursor(
                CHALLENGE_RECORD_ID,
                CommentStatus.BEFORE_RECORD,
                c1.getId(),
                Pageable.ofSize(3)
        );

        // then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("cursor 이후 댓글이 없으면 빈 리스트를 반환한다")
    void findActiveByChallengeRecordAfterCursor_returnsEmptyListWhenNoMoreComments() {
        // given
        Comment last = commentRepository.save(Comment.create(CHALLENGE_RECORD_ID, USER_ID, "마지막 댓글", CommentStatus.BEFORE_RECORD));

        // when
        List<Comment> result = commentRepository.findByChallengeRecordAfterCursor(
                CHALLENGE_RECORD_ID,
                CommentStatus.BEFORE_RECORD,
                last.getId(),
                Pageable.ofSize(20)
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("챌린지 기록과 댓글 상태가 같은 댓글 개수를 반환한다")
    void countActiveByChallengeRecord_returnsCorrectCount() {
        // given
        commentRepository.save(Comment.create(CHALLENGE_RECORD_ID, USER_ID, "인증 전 댓글1", CommentStatus.BEFORE_RECORD));
        commentRepository.save(Comment.create(CHALLENGE_RECORD_ID, OTHER_USER_ID, "인증 전 댓글2", CommentStatus.BEFORE_RECORD));
        commentRepository.save(Comment.create(CHALLENGE_RECORD_ID, USER_ID, "인증 후 댓글", CommentStatus.AFTER_RECORD));
        commentRepository.save(Comment.create(OTHER_CHALLENGE_RECORD_ID, USER_ID, "다른 챌린지 기록 댓글", CommentStatus.BEFORE_RECORD));

        // when
        long count = commentRepository.countByChallengeRecord(CHALLENGE_RECORD_ID, CommentStatus.BEFORE_RECORD);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("댓글이 없으면 count는 0이다")
    void countByChallengeRecord_returnsZeroWhenNoComments() {
        // when
        long count = commentRepository.countByChallengeRecord(CHALLENGE_RECORD_ID, CommentStatus.BEFORE_RECORD);

        // then
        assertThat(count).isZero();
    }

}
