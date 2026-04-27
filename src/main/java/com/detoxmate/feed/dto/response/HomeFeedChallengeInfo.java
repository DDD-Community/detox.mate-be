package com.detoxmate.feed.dto.response;

import java.time.Instant;


public record HomeFeedChallengeInfo(
        Long groupChallengeId,
        String groupChallengeName,
        Instant startAt,
        Integer streakCount
) {
}