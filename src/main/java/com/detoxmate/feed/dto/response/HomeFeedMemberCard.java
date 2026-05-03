package com.detoxmate.feed.dto.response;


import java.time.LocalDateTime;

public record HomeFeedMemberCard(
        Long userId,
        Long groupMemberId,
        String displayName,
        String profileImageUrl,
        Long challengeRecordId,
        String challengeStatus,
        String activityImageUrl,
        String oneLineReview,
        Integer totalUsedMinutes,
        String goalMinutes,
        Long activityRecordId,
        LocalDateTime verifiedAt,
        Integer reactionCount,
        Integer commentCount,
        Integer pokeCount,
        Boolean isPoked
) {
}
