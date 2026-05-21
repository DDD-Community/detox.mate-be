package com.detoxmate.docs.feed.mockdata;

import com.detoxmate.feed.dto.response.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FeedDetailMockData {

    public static FeedDetailResponse createFeedDetailResponse() {
        return new FeedDetailResponse(
                1000L,
                1L,
                2000L,
                "AFTER_RECORD_SUCCESS",
                LocalDate.of(2026, 5, 3),
                new FeedDetailAuthorInfo(
                        10L,
                        "민준",
                        "https://example.com/profiles/minjun.png",
                        false
                ),
                LocalDateTime.of(2026, 5, 3, 13, 0),
                "activity/image-1000.png",
                "오늘 인증 완료",
                FeedGoalStatus.SUCCESS,
                180,
                List.of(
                        new FeedDetailUsageDetail("INSTAGRAM", 40),
                        new FeedDetailUsageDetail("YOUTUBE", 80)
                ),
                new FeedDetailReactionSummary(
                        2,
                        List.of(
                                new FeedDetailReactionItem(
                                        "CLAP",
                                        20L,
                                        "Alice",
                                        "https://example.com/profiles/alice.png",
                                        false
                                ),
                                new FeedDetailReactionItem(
                                        "MUSCLE",
                                        21L,
                                        "Bob",
                                        "https://example.com/profiles/bob.png",
                                        false
                                )
                        )
                ),
                2,
                0,
                false,
                false,
                List.of()
        );
    }
}
