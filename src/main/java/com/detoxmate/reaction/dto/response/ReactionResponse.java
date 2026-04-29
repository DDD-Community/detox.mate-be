package com.detoxmate.reaction.dto.response;

import java.time.LocalDateTime;


public record ReactionResponse(
        Long reactionId,
        Long groupChallengeId,
        Long stampId,
        Long userId,
        String reactionBody,
        LocalDateTime createdAt
) {
}
