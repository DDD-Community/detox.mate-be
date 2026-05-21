package com.detoxmate.comment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 댓글 작성자 정보
 */
public record CommentAuthorInfo(
        Long userId,
        String displayName,
        String profileImageUrl,
        @JsonProperty("isUserWithdrawn")
        boolean userWithdrawn
) {
}
