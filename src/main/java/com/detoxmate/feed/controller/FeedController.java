package com.detoxmate.feed.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.feed.dto.response.FeedDetailResponse;
import com.detoxmate.feed.dto.response.GroupChallengeOverviewResponse;
import com.detoxmate.feed.dto.response.GroupChallengeRecordFeedResponse;
import com.detoxmate.feed.dto.response.HomeFeedResponse;
import com.detoxmate.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/group-challenges/{groupChallengeId}/overview")
    public ResponseEntity<GroupChallengeOverviewResponse> getGroupChallengeOverview(
            @PathVariable Long groupChallengeId,
            CurrentUser currentUser
    ) {
        return ResponseEntity.ok(
                feedService.getGroupChallengeOverview(groupChallengeId, currentUser.id())
        );
    }

    @Deprecated(since = "2026-05-09", forRemoval = false)
    @GetMapping("/group-challenges/{groupChallengeId}/home")
    public ResponseEntity<HomeFeedResponse> getHomeFeed(@PathVariable Long groupChallengeId,
                                                        CurrentUser currentUser) {
        return ResponseEntity.ok(
                feedService.getHomeFeed(groupChallengeId, currentUser.id())
        );
    }

    @GetMapping("/group-challenges/{groupChallengeId}/challenge-records/today")
    public ResponseEntity<GroupChallengeRecordFeedResponse> getTodayChallengeRecords(
            @PathVariable Long groupChallengeId,
            CurrentUser currentUser
    ) {
        return ResponseEntity.ok(
                feedService.getTodayChallengeRecordFeed(groupChallengeId, currentUser.id())
        );
    }

    @GetMapping("/group-challenges/{groupChallengeId}/challenge-records")
    public ResponseEntity<GroupChallengeRecordFeedResponse> getHistoryChallengeRecords(
            @PathVariable Long groupChallengeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            CurrentUser currentUser
    ) {
        return ResponseEntity.ok(
                feedService.getHistoryChallengeRecordFeed(groupChallengeId, date, currentUser.id())
        );
    }

    @GetMapping("/challenge-records/{challengeRecordId}")
    public ResponseEntity<FeedDetailResponse> getFeedDetail(@PathVariable Long challengeRecordId,
                                                            CurrentUser currentUser) {
        return ResponseEntity.ok(
                feedService.getFeedDetail(challengeRecordId, currentUser.id())
        );
    }

}
