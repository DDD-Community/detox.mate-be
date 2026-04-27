package com.detoxmate.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 댓글/답글 작성 요청
 */
public record CreateCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다")
        String commentMessage,

        Long parentCommentId
) {
}