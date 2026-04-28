package com.detoxmate.reaction.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.reaction.dto.request.CreateReactionRequest;
import com.detoxmate.reaction.dto.response.ReactionResponse;
import com.detoxmate.reaction.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group-challenges")
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/{groupChallengeId}/stamps/{stampId}/reactions")
    public ResponseEntity<ReactionResponse> createReaction(@PathVariable Long groupChallengeId,
                                                           @PathVariable Long stampId,
                                                           @Valid @RequestBody CreateReactionRequest request,
                                                           CurrentUser currentUser
    ) {
        ReactionResponse response = reactionService.create(
                groupChallengeId, stampId, request, currentUser.id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{groupChallengeId}/reactions/{reactionId}")
    public ResponseEntity<Void> deleteReaction(@PathVariable Long groupChallengeId,
                                               @PathVariable Long reactionId,
                                               CurrentUser currentUser
    ) {
        reactionService.delete(groupChallengeId, reactionId, currentUser.id());
        return ResponseEntity.noContent().build();
    }

}
