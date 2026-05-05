package com.detoxmate.docs.feed.mockdata;

import com.detoxmate.feed.dto.response.*;

import java.time.Instant;
import java.util.List;

public class FeedDetailMockData {

    public static FeedDetailResponse createFeedDetailResponse() {
        return new FeedDetailResponse(
                101L,
                1L,
                new FeedDetailAuthorInfo(
                        2L, "지수", "https://cdn.detoxmate.co.kr/profile/2.png"
                ),
                Instant.parse("2026-04-25T13:00:00Z"),
                "https://cdn.detoxmate.co.kr/activity/101.png",  // activityImageUrl
                "2시간동안 러닝 뛰고 온 날!",
                "SUCCESS",
                80,
                List.of(
                        new FeedDetailUsageDetail("ALL_USE", 70)
                ),
                new FeedDetailReactionSummary(
                        7,
                        List.of(
                                new FeedDetailReactionItem(
                                        "MUSCLE", 6L, "xeulbn",
                                        "https://cdn.detoxmate.co.kr/profile/6.png"
                                ),
                                new FeedDetailReactionItem(
                                        "MUSCLE", 2L, "의진",
                                        "https://cdn.detoxmate.co.kr/profile/2.png"
                                )
                        )
                ),
                10
        );
    }
}
