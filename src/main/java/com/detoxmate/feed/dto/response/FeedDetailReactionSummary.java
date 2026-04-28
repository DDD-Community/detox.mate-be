package com.detoxmate.feed.dto.response;

import java.util.List;


public record FeedDetailReactionSummary(
        Integer totalCount,
        List<FeedDetailReactionItem> summary
) {
}
