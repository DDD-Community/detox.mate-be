package com.detoxmate.docs.feed.mockdata;

import com.detoxmate.reaction.dto.response.ReactionResponse;

import java.time.LocalDateTime;

public class ReactionMockData {

    public static ReactionResponse createReactionResponse() {
        return new ReactionResponse(
                9001L,
                1L,
                1L,
                "CLAP",
                LocalDateTime.parse("2026-05-01T10:30:00")
        );
    }

}
