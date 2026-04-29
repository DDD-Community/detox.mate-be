package com.detoxmate.comment.dto.response;

import java.time.Instant;

/**
 * 댓글 목록 항목
 */
public record CommentItem(
        Long commentId,
        CommentAuthorInfo author,
        String commentBody,
        Instant createdAt
) {
}
