package com.detoxmate.comment.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.comment.CommentErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    private static final int MAX_BODY_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_record_id", nullable = false)
    private Long activityRecordId;

    @Column(name = "group_challenge_id",nullable = false)
    private Long groupChallengeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_body", nullable = false, length = MAX_BODY_LENGTH)
    private String commentBody;

    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Comment(Long activityRecordId, Long groupChallengeId, Long userId, String commentBody) {
        this.activityRecordId = activityRecordId;
        this.groupChallengeId = groupChallengeId;
        this.userId = userId;
        this.commentBody = commentBody;
        this.createdAt = LocalDateTime.now();
    }

    public static Comment create(Long activityRecordId, Long groupChallengeId, Long userId, String commentBody) {
        validateCommentBody(commentBody);
        return new Comment(activityRecordId, groupChallengeId, userId, commentBody);
    }

    private static void validateCommentBody(String commentBody) {
        if(commentBody==null || commentBody.isBlank()) {
            throw new CustomException(CommentErrorCode.COMMENT_BODY_REQUIRED);
        }

        if (commentBody.length() > MAX_BODY_LENGTH) {
            throw new CustomException(CommentErrorCode.COMMENT_BODY_LENGTH_EXCEEDED);
        }
    }

}
