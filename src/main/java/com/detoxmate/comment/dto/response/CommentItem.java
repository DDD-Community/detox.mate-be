package com.detoxmate.comment.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 댓글 목록 항목
 */
public record CommentItem(
        Long commentId,
        CommentAuthorInfo author,
        String body,
        List<RelatedCommentDto> relatedComment,
        Instant createdAt,
        Integer replyCount
) {
}