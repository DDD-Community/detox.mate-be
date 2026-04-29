package com.detoxmate.comment.repository;

import com.detoxmate.comment.domain.Comment;
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

    private static final Long ANY_ACTIVITY_RECORD_ID = 100L;
    private static final Long ANY_GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_ACTIVITY_RECORD_ID = 200L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;
    private static final Long ANY_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
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

        //when
        Comment saved = commentRepository.saveAndFlush(comment);

        //then
        assertThat(saved.getActivityRecordId()).isEqualTo(ANY_ACTIVITY_RECORD_ID);
        assertThat(saved.getGroupChallengeId()).isEqualTo(ANY_GROUP_CHALLENGE_ID);
        assertThat(saved.getUserId()).isEqualTo(ANY_USER_ID);
        assertThat(saved.getCommentBody()).isEqualTo(VALID_BODY);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("첫 페이지 조회 시 같은 그룹 챌린지와 activity record의 댓글만 ID 오름차순으로 반환된다")
    void findFirstPage_returnsOnlyMatchingCommentsOrderedByIdAsc() {
        //given
        Comment c1 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글1"));
        Comment c2 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, OTHER_USER_ID, "댓글2"));
        commentRepository.save(Comment.create(OTHER_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "다른 activity record 댓글"));
        commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, OTHER_GROUP_CHALLENGE_ID, ANY_USER_ID, "다른 group challenge 댓글"));
        Comment c3 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글3"));

        //when
        List<Comment> result = commentRepository.findByGroupChallengeIdAndActivityRecordIdOrderByIdAsc(
                ANY_GROUP_CHALLENGE_ID,
                ANY_ACTIVITY_RECORD_ID,
                Pageable.ofSize(20)
        );

        //then
        assertThat(result)
                .extracting(Comment::getId)
                .containsExactly(c1.getId(), c2.getId(), c3.getId());
    }

    @Test
    @DisplayName("첫 페이지 조회 시 size만큼만 반환된다")
    void findFirstPage_limitsBySize() {
        //given
        for (int i=0; i<5; i++) {
            commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글" + i));
        }

        //when
        List<Comment> result = commentRepository.findByGroupChallengeIdAndActivityRecordIdOrderByIdAsc(
                ANY_GROUP_CHALLENGE_ID,
                ANY_ACTIVITY_RECORD_ID,
                Pageable.ofSize(3)
        );

        //then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("댓글이 없는 activity record는 첫 페이지가 빈 리스트로 반환된다")
    void findFirstPage_returnsEmptyListWhenNoComments() {
        //given & when
        List<Comment> result = commentRepository.findByGroupChallengeIdAndActivityRecordIdOrderByIdAsc(
                ANY_GROUP_CHALLENGE_ID,
                999L,
                Pageable.ofSize(20)
        );

        //then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("cursor 이후의 댓글만 ID 오름차순으로 반환된다")
    void findAfterCursor_returnsOnlyCommentsAfterCursor() {
        //given
        Comment c1 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글1"));
        Comment c2 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글2"));
        Comment c3 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글3"));

        //when
        List<Comment> result = commentRepository.findByGroupChallengeIdAndActivityRecordIdAndIdGreaterThanOrderByIdAsc(
                ANY_GROUP_CHALLENGE_ID,
                ANY_ACTIVITY_RECORD_ID,
                c1.getId(),
                Pageable.ofSize(20)
        );

        //then
        assertThat(result)
                .extracting(Comment::getId)
                .containsExactly(c2.getId(), c3.getId());
    }

    @Test
    @DisplayName("cursor 이후 조회 시에도 size만큼만 반환된다")
    void findAfterCursor_limitsBySize() {
        //given
        Comment c1 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "기준"));
        for (int i=0; i<5; i++) {
            commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글" + i));
        }

        //when
        List<Comment> result = commentRepository.findByGroupChallengeIdAndActivityRecordIdAndIdGreaterThanOrderByIdAsc(
                ANY_GROUP_CHALLENGE_ID,
                ANY_ACTIVITY_RECORD_ID,
                c1.getId(),
                Pageable.ofSize(3)
        );

        //then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("cursor 이후 댓글이 없으면 빈 리스트가 반환된다")
    void findAfterCursor_returnsEmptyListWhenNoMoreComments() {
        //given
        Comment last = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "마지막"));

        //when
        List<Comment> result = commentRepository.findByGroupChallengeIdAndActivityRecordIdAndIdGreaterThanOrderByIdAsc(
                ANY_GROUP_CHALLENGE_ID,
                ANY_ACTIVITY_RECORD_ID,
                last.getId(),
                Pageable.ofSize(20)
        );

        //then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("cursor 이후 조회 시 다른 activity record나 group challenge 댓글은 포함되지 않는다")
    void findAfterCursor_excludesOtherActivityRecordsAndGroupChallenges() {
        //given
        Comment c1 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글1"));
        commentRepository.save(Comment.create(OTHER_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "다른 activity record"));
        commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, OTHER_GROUP_CHALLENGE_ID, ANY_USER_ID, "다른 group challenge"));
        Comment c2 = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, ANY_USER_ID, "댓글2"));

        //when
        List<Comment> result = commentRepository.findByGroupChallengeIdAndActivityRecordIdAndIdGreaterThanOrderByIdAsc(
                ANY_GROUP_CHALLENGE_ID,
                ANY_ACTIVITY_RECORD_ID,
                c1.getId(),
                Pageable.ofSize(20)
        );

        //then
        assertThat(result)
                .extracting(Comment::getId)
                .containsExactly(c2.getId());
    }

    @Test
    @DisplayName("그룹 챌린지와 activity record의 댓글 개수가 정확히 반환된다")
    void countByGroupChallengeIdAndActivityRecordId_returnsCorrectCount() {
        //given
        commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, 1L, "댓글1"));
        commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, 2L, "댓글2"));
        commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, 3L, "댓글3"));
        commentRepository.save(Comment.create(OTHER_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, 1L, "다른 activity record"));
        commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, OTHER_GROUP_CHALLENGE_ID, 1L, "다른 group challenge"));

        //when
        long count = commentRepository.countByGroupChallengeIdAndActivityRecordId(
                ANY_GROUP_CHALLENGE_ID,
                ANY_ACTIVITY_RECORD_ID
        );

        //then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("댓글이 없는 그룹 챌린지와 activity record의 댓글 개수는 0이다")
    void countByGroupChallengeIdAndActivityRecordId_returnsZeroWhenNoComments() {
        //given & when
        long count = commentRepository.countByGroupChallengeIdAndActivityRecordId(
                ANY_GROUP_CHALLENGE_ID,
                999L
        );

        //then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("같은 activity record라도 다른 group challenge의 댓글은 분리되어 조회된다")
    void findFirstPage_isolatesByGroupChallenge() {
        //given
        Comment a = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, 1L, "수능방"));
        commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, OTHER_GROUP_CHALLENGE_ID, 2L, "직장인방"));
        Comment c = commentRepository.save(Comment.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, 3L, "수능방2"));

        //when
        List<Comment> result = commentRepository.findByGroupChallengeIdAndActivityRecordIdOrderByIdAsc(
                ANY_GROUP_CHALLENGE_ID, ANY_ACTIVITY_RECORD_ID, Pageable.ofSize(20));

        //then
        assertThat(result)
                .extracting(Comment::getId)
                .containsExactly(a.getId(), c.getId());
    }

}
