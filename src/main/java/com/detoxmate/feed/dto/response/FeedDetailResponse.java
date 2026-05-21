package com.detoxmate.feed.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public record FeedDetailResponse(
        Long challengeRecordId,
        Long groupChallengeId,
        Long activityRecordId,
        String challengeStatus,
        LocalDate recordDate,
        FeedDetailAuthorInfo author,
        LocalDateTime activityCreatedAt,
        String activityImageUrl,
        String oneLineReview,
        FeedGoalStatus goalStatus,
        Integer snapshotGoalMinutes,
        List<FeedDetailUsageDetail> details,
        FeedDetailReactionSummary reactions,
        Integer commentCount,
        Integer pokeCount,
        Boolean pokeable,
        Boolean poked,
        List<FeedDetailPokedUser> pokedUsers
) {
}
