package com.detoxmate.comment.dto.response;

/**
 * 댓글 작성자 정보
 */
public record CommentAuthorInfo(
        Long userId,
        String displayName,
        String profileImageUrl
) {
}
