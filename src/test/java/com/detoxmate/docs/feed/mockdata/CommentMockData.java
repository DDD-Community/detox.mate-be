package com.detoxmate.docs.feed.mockdata;

import com.detoxmate.comment.dto.response.*;

import java.time.Instant;
import java.util.List;

public class CommentMockData {

    public static CommentListResponse createCommentListResponse() {
        return new CommentListResponse(
                123L,
                List.of(
                        new CommentItem(
                                1L,
                                new CommentAuthorInfo(
                                        3L, "민준",
                                        "https://cdn.detoxmate.co.kr/profile/3.png"
                                ),
                                "와 대박! 오늘도 성공하셨네 독하다 독해",
                                List.of(
                                        new RelatedCommentDto(
                                                2L,
                                                new CommentAuthorInfo(
                                                        4L, "지수",
                                                        "https://cdn.detoxmate.co.kr/profile/4.png"
                                                ),
                                                "ㄹㅇ 멋있어요",
                                                Instant.parse("2026-04-26T11:00:00Z")
                                        )
                                ),
                                Instant.parse("2026-04-26T10:00:00Z"),
                                1
                        )
                ),
                "eyJpZCI6MX0="
        );
    }

    public static CommentResponse createCommentResponse() {
        return new CommentResponse(
                1L,
                1L,
                101L,
                3L,
                null,
                "엘렐렐렐레 인증 안해? 안해? 안해?!!",
                Instant.parse("2026-04-26T10:00:00Z")
        );
    }

    public static CommentResponse createReplyResponse() {
        return new CommentResponse(
                2L,
                1L,
                101L,
                4L,
                1L,
                "ㄹㅇ 존멋.",
                Instant.parse("2026-04-26T11:00:00Z")
        );
    }
}
