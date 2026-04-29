package com.detoxmate.comment.service;

import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.dto.response.CommentListResponse;
import com.detoxmate.comment.dto.response.CommentResponse;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    public CommentListResponse list(Long groupChallengeId, Long activityRecordId, String cursor, int size) {
        throw new UnsupportedOperationException("아직 미구현 - API 문서화 단계");
    }

    public CommentResponse create(Long groupChallengeId, Long activityRecordId,
                                  CreateCommentRequest request, Long currentUserId) {
        throw new UnsupportedOperationException("아직 미구현 - API 문서화 단계");
    }
}
