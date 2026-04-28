package com.detoxmate.comment.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.dto.response.CommentListResponse;
import com.detoxmate.comment.dto.response.CommentResponse;
import com.detoxmate.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group-challenges")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{groupChallengeId}/stamps/{stampId}/comments")
    public ResponseEntity<CommentListResponse> getComments(@PathVariable Long groupChallengeId,
                                                           @PathVariable Long stampId,
                                                           @RequestParam(required = false) String cursor,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                commentService.list(groupChallengeId, stampId, cursor, size)
        );
    }

    @PostMapping("/{groupChallengeId}/stamps/{stampId}/comments")
    public ResponseEntity<CommentResponse> createComment(@PathVariable Long groupChallengeId,
                                                         @PathVariable Long stampId,
                                                         @Valid @RequestBody CreateCommentRequest request,
                                                         CurrentUser currentUser) {

        CommentResponse response = commentService.create(
                groupChallengeId, stampId, request, currentUser.id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
