package com.detoxmate.comment.dto.response;

import java.time.Instant;

/**
 * 답글 정보
 */
public record RelatedCommentDto(
        Long commentId,
        CommentAuthorInfo author,
        String body,
        Instant createdAt
) {
}