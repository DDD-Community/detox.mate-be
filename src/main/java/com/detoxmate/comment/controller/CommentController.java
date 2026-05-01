package com.detoxmate.comment.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.dto.response.CommentListResponse;
import com.detoxmate.comment.dto.response.CommentResponse;
import com.detoxmate.comment.service.CommentService;
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
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{challengeRecordId}/comments")
    public ResponseEntity<CommentListResponse> getComments(@PathVariable Long challengeRecordId,
                                                           @RequestParam(required = false) String cursor,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           CurrentUser currentUser) {
        log.info("[Comment][get-comments] challengeRecordId={}, size={}, userId={}",
                challengeRecordId, size, currentUser.id());

        CommentListResponse response = commentService.list(challengeRecordId, cursor, size);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{challengeRecordId}/comments")
    public ResponseEntity<CommentResponse> createComment(@PathVariable Long challengeRecordId,
                                                         @Valid @RequestBody CreateCommentRequest request,
                                                         CurrentUser currentUser) {
        log.info("[Comment][create-comment] challengeRecordId={}, userId={}",
                challengeRecordId, currentUser.id());

        CommentResponse response = commentService.create(challengeRecordId, request, currentUser.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
