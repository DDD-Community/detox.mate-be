package com.detoxmate.comment.dto.response;

import java.time.Instant;

/**
 * 댓글 작성 응답
 */
public record CommentResponse(
        Long commentId,
        Long groupChallengeId,
        Long stampId,
        Long userId,
        Long parentCommentId,
        String commentMessage,
        Instant createdAt
) {
}
