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
@RequestMapping("/challenge-records")
@Slf4j
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/{challengeRecordId}/reactions")
    public ResponseEntity<ReactionResponse> createReaction(@PathVariable Long challengeRecordId,
                                                           @Valid @RequestBody CreateReactionRequest request,
                                                           CurrentUser currentUser) {
        log.info(
                "[Reaction][create-reaction] userId={}, challengeRecordId={}, request={}",
                currentUser.id(), challengeRecordId, request);

        ReactionResponse response = reactionService.create(challengeRecordId, request, currentUser.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{challengeRecordId}/reactions/{reactionId}")
    public ResponseEntity<Void> deleteReaction(@PathVariable Long challengeRecordId,
                                               @PathVariable Long reactionId,
                                               CurrentUser currentUser) {
        log.info(
                "[Reaction][delete-reaction] userId={}, challengeRecordId={}, reactionId={}",
                currentUser.id(), challengeRecordId, reactionId);

        reactionService.delete(challengeRecordId, reactionId, currentUser.id());

        return ResponseEntity.noContent().build();
    }

}
