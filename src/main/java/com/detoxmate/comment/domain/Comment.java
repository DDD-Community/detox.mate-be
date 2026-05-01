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
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "challenge_record_id", nullable = false)
    private Long challengeRecordId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_body", nullable = false, length = MAX_BODY_LENGTH)
    private String commentBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_status", nullable = false, length = 20)
    private CommentStatus commentStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Comment(Long challengeRecordId, Long userId, String commentBody, CommentStatus commentStatus) {
        this.challengeRecordId = challengeRecordId;
        this.userId = userId;
        this.commentBody = commentBody;
        this.commentStatus = commentStatus;
        this.createdAt = LocalDateTime.now();
    }

    public static Comment create(
            Long challengeRecordId,
            Long userId,
            String commentBody,
            CommentStatus commentStatus
    ) {
        validate(challengeRecordId, commentBody, commentStatus);
        return new Comment(challengeRecordId, userId, commentBody, commentStatus);
    }

    private static void validate(Long challengeRecordId, String commentBody, CommentStatus commentStatus) {
        if (challengeRecordId == null) {
            throw new CustomException(CommentErrorCode.COMMENT_CHALLENGE_RECORD_REQUIRED);
        }

        if (commentBody == null || commentBody.isBlank()) {
            throw new CustomException(CommentErrorCode.COMMENT_BODY_REQUIRED);
        }

        if (commentBody.length() > MAX_BODY_LENGTH) {
            throw new CustomException(CommentErrorCode.COMMENT_BODY_LENGTH_EXCEEDED);
        }

        if (commentStatus == null) {
            throw new CustomException(CommentErrorCode.COMMENT_STATUS_REQUIRED);
        }
    }

}
