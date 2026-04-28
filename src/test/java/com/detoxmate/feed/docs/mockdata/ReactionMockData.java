package com.detoxmate.feed.docs.mockdata;

import com.detoxmate.reaction.dto.response.ReactionResponse;

import java.time.Instant;

public class ReactionMockData {

    public static ReactionResponse createReactionResponse() {
        return new ReactionResponse(
                9001L,
                1L,
                101L,
                1L,
                "MUSCLE",
                Instant.parse("2026-04-26T10:00:00Z")
        );
    }
}
