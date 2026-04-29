package com.detoxmate.comment.repository;


import com.detoxmate.comment.domain.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByGroupChallengeIdAndActivityRecordIdOrderByIdAsc(
            Long groupChallengeId, Long activityRecordId, Pageable pageable);

    List<Comment> findByGroupChallengeIdAndActivityRecordIdAndIdGreaterThanOrderByIdAsc(
            Long groupChallengeId, Long activityRecordId, Long cursorId, Pageable pageable);

    long countByGroupChallengeIdAndActivityRecordId(
            Long groupChallengeId, Long activityRecordId);
}
