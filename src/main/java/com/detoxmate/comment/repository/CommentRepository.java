package com.detoxmate.comment.repository;


import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.domain.CommentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
            select c
            from Comment c
            where c.challengeRecordId = :challengeRecordId
              and c.commentStatus = :commentStatus
            order by c.id asc
            """)
    List<Comment> findByChallengeRecord(Long challengeRecordId, CommentStatus commentStatus, Pageable pageable);

    @Query("""
            select c
            from Comment c
            where c.challengeRecordId = :challengeRecordId
              and c.commentStatus = :commentStatus
              and c.id > :cursorId
            order by c.id asc
            """)
    List<Comment> findByChallengeRecordAfterCursor(
            Long challengeRecordId,
            CommentStatus commentStatus,
            Long cursorId,
            Pageable pageable
    );

    @Query("""
            select count(c)
            from Comment c
            where c.challengeRecordId = :challengeRecordId
              and c.commentStatus = :commentStatus
            """)
    long countByChallengeRecord(Long challengeRecordId, CommentStatus commentStatus);
}
