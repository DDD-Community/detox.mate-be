package com.detoxmate.feed.service;

import com.detoxmate.feed.dto.response.HomeFeedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedService {


    @Transactional(readOnly = true)
    public HomeFeedResponse getHomeFeed(Long groupChallengeId, Long currentUserId) {
        throw new RuntimeException("Not yet implemented");
    }


}
