package com.detoxmate.comment.dto.response;

import java.util.List;

/**
 * 댓글 목록 조회 응답
 */
public record CommentListResponse(
        Long totalCount,
        List<CommentItem> items,
        String nextCursor
) {
}
