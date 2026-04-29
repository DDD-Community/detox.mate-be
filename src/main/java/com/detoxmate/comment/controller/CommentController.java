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
@RequestMapping("/group-challenges")
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{groupChallengeId}/activity-records/{activityRecordId}/comments")
    public ResponseEntity<CommentListResponse> getComments(@PathVariable Long groupChallengeId,
                                                           @PathVariable Long activityRecordId,
                                                           @RequestParam(required = false) String cursor,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           CurrentUser currentUser) {
        log.info("[Comment][get-comments] groupChallengeId={}, activityRecordId={}, size={}, userId = {}",groupChallengeId, activityRecordId,size,currentUser.id());

        return ResponseEntity.ok(commentService.list(groupChallengeId, activityRecordId, cursor, size));
    }

    @PostMapping("/{groupChallengeId}/activity-records/{activityRecordId}/comments")
    public ResponseEntity<CommentResponse> createComment(@PathVariable Long groupChallengeId,
                                                         @PathVariable Long activityRecordId,
                                                         @Valid @RequestBody CreateCommentRequest request,
                                                         CurrentUser currentUser) {
        log.info("[Comment][create-comment] groupChallengeId={}, activityRecordId={}, userId ={}", groupChallengeId, activityRecordId, currentUser.id());
        CommentResponse response = commentService.create(groupChallengeId, activityRecordId, request, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
