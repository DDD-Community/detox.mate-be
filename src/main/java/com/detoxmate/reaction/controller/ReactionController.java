package com.detoxmate.reaction.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.reaction.dto.request.CreateReactionRequest;
import com.detoxmate.reaction.dto.response.ReactionResponse;
import com.detoxmate.reaction.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group-challenges")
@Slf4j
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/{groupChallengeId}/activity-records/{activityRecordId}/reactions")
    public ResponseEntity<ReactionResponse> createReaction(@PathVariable Long groupChallengeId,
                                                           @PathVariable Long activityRecordId,
                                                           @Valid @RequestBody CreateReactionRequest request,
                                                           CurrentUser currentUser) {

        log.info("[Reaction][create-reaction] userId={}, groupChallengeId={}, activityRecordId={}, request={}", currentUser.id(), groupChallengeId, activityRecordId, request);
        ReactionResponse response = reactionService.create(
                groupChallengeId, activityRecordId, request, currentUser.id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{groupChallengeId}/reactions/{reactionId}")
    public ResponseEntity<Void> deleteReaction(@PathVariable Long groupChallengeId,
                                               @PathVariable Long reactionId,
                                               CurrentUser currentUser) {
        log.info("[Reaction][delete-reaction] userId = {}, groupChallengeId={}, reactionId={}",currentUser.id(), groupChallengeId, reactionId);
        reactionService.delete(groupChallengeId, reactionId, currentUser.id());

        return ResponseEntity.noContent().build();
    }

}
