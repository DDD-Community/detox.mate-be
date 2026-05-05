package com.detoxmate.feed.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.feed.dto.response.FeedDetailResponse;
import com.detoxmate.feed.dto.response.HomeFeedResponse;
import com.detoxmate.feed.service.FeedDetailService;
import com.detoxmate.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group-challenges")
public class FeedController {

    private final FeedService feedService;
    private final FeedDetailService feedDetailService;

    @GetMapping("/{groupChallengeId}/home")
    public ResponseEntity<HomeFeedResponse> getHomeFeed(@PathVariable Long groupChallengeId,
                                                        CurrentUser currentUser) {
        return ResponseEntity.ok(
                feedService.getHomeFeed(groupChallengeId, currentUser.id())
        );
    }

    @GetMapping("/{groupChallengeId}/stamps/{stampId}")
    public ResponseEntity<FeedDetailResponse> getStampDetail(@PathVariable Long groupChallengeId,
                                                             @PathVariable Long stampId,
                                                             CurrentUser currentUser) {
        return ResponseEntity.ok(
                feedDetailService.getFeedDetail(groupChallengeId, stampId, currentUser.id())
        );
    }

}
