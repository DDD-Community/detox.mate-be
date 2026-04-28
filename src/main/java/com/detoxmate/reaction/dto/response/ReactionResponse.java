package com.detoxmate.reaction.dto.response;

import java.time.Instant;


public record ReactionResponse(
        Long reactionId,
        Long groupChallengeId,
        Long stampId,
        Long userId,
        String reactionBody,
        Instant createdAt
) {
}
