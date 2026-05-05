package com.detoxmate.feed.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;


public record HomeFeedChallengeInfo(
        Long groupChallengeId,
        String groupChallengeName,
        LocalDateTime startAt,
        Integer streakCount
) {
}
