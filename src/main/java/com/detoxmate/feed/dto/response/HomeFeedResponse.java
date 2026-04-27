package com.detoxmate.feed.dto.response;

import java.util.List;


public record HomeFeedResponse(
        HomeFeedChallengeInfo challenge,
        List<HomeFeedMemberCard> members
) {
}