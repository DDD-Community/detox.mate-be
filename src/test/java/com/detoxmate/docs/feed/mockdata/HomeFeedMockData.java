package com.detoxmate.docs.feed.mockdata;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.feed.dto.response.GroupChallengeOverviewResponse;
import com.detoxmate.feed.dto.response.GroupChallengeRecordFeedResponse;
import com.detoxmate.feed.dto.response.HomeFeedChallengeInfo;
import com.detoxmate.feed.dto.response.HomeFeedMemberCard;
import com.detoxmate.feed.dto.response.HomeFeedResponse;
import com.detoxmate.group.dto.ActivityRecordDetailHistoryResponse;
import com.detoxmate.group.dto.GroupDailyVerificationSummaryResponse;
import com.detoxmate.group.dto.MemberDailyGoalResponse;

import java.time.LocalDate;
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
                                false,
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
                                true,
                                false
                        )
                )
        );
    }

    public static GroupChallengeRecordFeedResponse createGroupChallengeRecordFeedResponse() {
        return new GroupChallengeRecordFeedResponse(
                10L,
                LocalDate.of(2026, 5, 3),
                new GroupDailyVerificationSummaryResponse(
                        LocalDate.of(2026, 5, 3),
                        "IN_PROGRESS",
                        null,
                        2,
                        1,
                        1
                ),
                List.of(
                        new GroupChallengeRecordFeedResponse.MemberResponse(
                                100L,
                                1000L,
                                10L,
                                "민준",
                                "https://example.com/profiles/minjun.png",
                                false,
                                false,
                                "ACTIVE",
                                "JOINED",
                                "GOAL_ACHIEVED",
                                true,
                                List.of(new MemberDailyGoalResponse(
                                        900L,
                                        UsageGoalTypeCode.TOTAL_USAGE,
                                        120,
                                        LocalDate.of(2026, 5, 3)
                                )),
                                10000L,
                                new GroupChallengeRecordFeedResponse.ActivityRecordResponse(
                                        LocalDateTime.of(2026, 5, 3, 13, 0),
                                        "https://example.com/activity-records/10000.png",
                                        "오늘 인증 완료",
                                        true,
                                        List.of(new ActivityRecordDetailHistoryResponse(
                                                UsageGoalTypeCode.TOTAL_USAGE,
                                                90,
                                                120,
                                                true
                                        ))
                                ),
                                4,
                                2,
                                0,
                                false,
                                null,
                                null,
                                null
                        ),
                        new GroupChallengeRecordFeedResponse.MemberResponse(
                                101L,
                                1001L,
                                11L,
                                "서연",
                                "https://example.com/profiles/seoyeon.png",
                                false,
                                false,
                                "ACTIVE",
                                "JOINED",
                                "NOT_CERTIFIED",
                                true,
                                List.of(),
                                10001L,
                                null,
                                0,
                                1,
                                3,
                                true,
                                null,
                                null,
                                null
                        )
                )
        );
    }

    public static GroupChallengeRecordFeedResponse.MemberResponse createGroupChallengeRecordFeedDetailResponse() {
        return new GroupChallengeRecordFeedResponse.MemberResponse(
                100L,
                1000L,
                10L,
                "민준",
                "https://example.com/profiles/minjun.png",
                false,
                false,
                "ACTIVE",
                "JOINED",
                "GOAL_ACHIEVED",
                true,
                List.of(new MemberDailyGoalResponse(
                        900L,
                        UsageGoalTypeCode.TOTAL_USAGE,
                        120,
                        LocalDate.of(2026, 5, 3)
                )),
                10000L,
                new GroupChallengeRecordFeedResponse.ActivityRecordResponse(
                        LocalDateTime.of(2026, 5, 3, 13, 0),
                        "https://example.com/activity-records/10000.png",
                        "오늘 인증 완료",
                        true,
                        List.of(new ActivityRecordDetailHistoryResponse(
                                UsageGoalTypeCode.TOTAL_USAGE,
                                90,
                                120,
                                true
                        ))
                ),
                4,
                2,
                0,
                false,
                false,
                List.of(),
                new GroupChallengeRecordFeedResponse.ReactionSummaryResponse(
                        1,
                        List.of(new GroupChallengeRecordFeedResponse.ReactionResponse(
                                "CLAP",
                                11L,
                                "서연",
                                "https://example.com/profiles/seoyeon.png",
                                false
                        ))
                )
        );
    }
}
