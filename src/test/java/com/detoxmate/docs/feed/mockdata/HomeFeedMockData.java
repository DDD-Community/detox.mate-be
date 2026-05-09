package com.detoxmate.docs.feed.mockdata;

import com.detoxmate.feed.dto.response.GroupChallengeOverviewResponse;
import com.detoxmate.feed.dto.response.HomeFeedChallengeInfo;
import com.detoxmate.feed.dto.response.HomeFeedMemberCard;
import com.detoxmate.feed.dto.response.HomeFeedResponse;

import java.time.LocalDateTime;
import java.util.List;

public class HomeFeedMockData {

    public static GroupChallengeOverviewResponse createGroupChallengeOverviewResponse() {
        return new GroupChallengeOverviewResponse(
                1L,
                10L,
                "수능방",
                3,
                "ACTIVE",
                LocalDateTime.of(2026, 5, 3, 9, 0),
                null,
                3
        );
    }

    public static HomeFeedResponse createHomeFeedResponse() {
        return new HomeFeedResponse(
                new HomeFeedChallengeInfo(
                        1L,
                        "수능방",
                        LocalDateTime.of(2026, 5, 3, 9, 0),
                        3
                ),
                List.of(
                        new HomeFeedMemberCard(
                                10L,
                                100L,
                                "민준",
                                "https://example.com/profiles/minjun.png",
                                1000L,
                                "AFTER_RECORD_SUCCESS",
                                "activity/image-1000.png",
                                "오늘 인증 완료",
                                120,
                                null,
                                2000L,
                                LocalDateTime.of(2026, 5, 3, 13, 0),
                                4,
                                2,
                                0,
                                false
                        ),
                        new HomeFeedMemberCard(
                                11L,
                                101L,
                                "서연",
                                "https://example.com/profiles/seoyeon.png",
                                1001L,
                                "BEFORE_RECORD",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                0,
                                1,
                                3,
                                true
                        )
                )
        );
    }
}
