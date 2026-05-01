package com.detoxmate.docs.feed.mockdata;

import com.detoxmate.feed.dto.response.HomeFeedChallengeInfo;
import com.detoxmate.feed.dto.response.HomeFeedMemberCard;
import com.detoxmate.feed.dto.response.HomeFeedResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class HomeFeedMockData {

    public static HomeFeedResponse createHomeFeedResponse() {
        return new HomeFeedResponse(
                new HomeFeedChallengeInfo(
                        1L,
                        "수능 100일 전 모임",
                        LocalDateTime.parse("2026-04-20T00:00:00Z"),
                        7
                ),
                List.of(
                        new HomeFeedMemberCard(
                                1L, 11L, "강슬빈",
                                "https://cdn.detoxmate.co.kr/profile/1.png",
                                "VERIFIED",
                                "https://cdn.detoxmate.co.kr/acting/1.png",
                                "2시간 러닝 뛰고 옴",
                                70,
                                "8H 30M",
                                101L,
                                3, 5, 0,
                                false
                        ),
                        new HomeFeedMemberCard(
                                2L, 12L, "김지호",
                                "https://cdn.detoxmate.co.kr/profile/2.png",
                                "NOT_YET",
                                null, null, null,
                                "8H 30M",
                                null,
                                0, 0, 1,
                                true
                        )
                )
        );
    }
}
