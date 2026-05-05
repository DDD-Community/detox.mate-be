package com.detoxmate.reaction.service;

import com.detoxmate.reaction.dto.request.CreateReactionRequest;
import com.detoxmate.reaction.dto.response.ReactionResponse;
import org.springframework.stereotype.Service;

@Service
public class ReactionService {

    public ReactionResponse create(Long groupChallengeId, Long stampId,
                                   CreateReactionRequest request, Long currentUserId) {
        throw new UnsupportedOperationException("아직 미구현 - API 문서화 단계");
    }

    public void delete(Long groupChallengeId, Long reactionId, Long currentUserId) {
        throw new UnsupportedOperationException("아직 미구현 - API 문서화 단계");
    }
}
