package com.detoxmate.reaction.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateReactionRequest(
        @NotBlank(message = "리액션 코드는 필수입니다")
        String reactionCode
) {
}