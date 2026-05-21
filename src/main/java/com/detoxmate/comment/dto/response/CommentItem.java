package com.detoxmate.comment.dto.response;

import java.time.LocalDateTime;

/**
 * 댓글 목록 항목
 */
public record CommentItem(
        Long commentId,
        CommentAuthorInfo author,
        String commentBody,
        LocalDateTime createdAt
) {
}
