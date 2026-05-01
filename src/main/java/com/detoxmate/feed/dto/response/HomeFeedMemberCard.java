package com.detoxmate.feed.dto.response;


public record HomeFeedMemberCard(
        Long userId,
        Long groupMemberId,
        String displayName,
        String profileImageUrl,
        String challengeStatus,
        String activityImageUrl,
        String oneLineReview,
        Integer totalUsedMinutes,
        String goalMinutes,
        Long activityRecordId,
        Integer reactionCount,
        Integer commentCount,
        Integer pokeCount,
        Boolean isPoked
) {
}
