package com.detoxmate.comment.dto.response;

import java.time.LocalDateTime;

/**
 * 댓글 작성 응답
 */
public record CommentResponse(
        Long commentId,
        Long groupChallengeId,
        Long activityRecordId,
        Long userId,
        String commentBody,
        LocalDateTime createdAt
) {
}
