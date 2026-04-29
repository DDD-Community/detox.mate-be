package com.detoxmate.comment.repository;


import com.detoxmate.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByActivityRecordIdOrderByCreatedAtAsc(Long activityRecordId);
}
