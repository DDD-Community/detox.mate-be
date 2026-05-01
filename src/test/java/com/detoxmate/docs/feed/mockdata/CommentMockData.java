package com.detoxmate.docs.feed.mockdata;

import com.detoxmate.comment.dto.response.*;

import java.time.LocalDateTime;
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
                                LocalDateTime.parse("2026-04-26T10:00:00")
                        )
                ),
                "eyJpZCI6MX0="
        );
    }


}
