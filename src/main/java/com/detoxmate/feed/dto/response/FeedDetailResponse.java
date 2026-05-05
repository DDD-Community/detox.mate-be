package com.detoxmate.feed.dto.response;

import java.time.Instant;
import java.util.List;


public record FeedDetailResponse(
        Long stampId,
        Long groupChallengeId,
        FeedDetailAuthorInfo author,
        Instant createdAt,
        String activityImageUrl,
        String oneLineReview,
        String goalStatus,
        Integer snapshotGoalMinutes,
        List<FeedDetailUsageDetail> details,
        FeedDetailReactionSummary reactions,
        Integer commentCount
) {
}
