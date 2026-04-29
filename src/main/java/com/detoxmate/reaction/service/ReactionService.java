package com.detoxmate.reaction.service;

import com.detoxmate.activityrecordchallengestatus.service.ActivityRecordChallengeStatusService;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.reaction.ReactionErrorCode;
import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.domain.ReactionBody;
import com.detoxmate.reaction.dto.request.CreateReactionRequest;
import com.detoxmate.reaction.dto.response.ReactionResponse;
import com.detoxmate.reaction.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final ActivityRecordChallengeStatusService statusService;

    @Transactional
    public ReactionResponse create(Long groupChallengeId, Long activityRecordId, CreateReactionRequest request, Long currentUserId) {
        ReactionBody body = ReactionBody.valueOf(request.reactionCode());

        if (reactionRepository.existsActiveReaction(groupChallengeId, activityRecordId, currentUserId, body)) {
            throw new CustomException(ReactionErrorCode.REACTION_ALREADY_EXISTS);
        }

        Reaction reaction = Reaction.create(activityRecordId, groupChallengeId, currentUserId, body);

        Reaction saved = reactionRepository.save(reaction);
        statusService.increaseReactionCount(groupChallengeId, activityRecordId);

        return toReactionResponse(saved);
    }

    @Transactional
    public void delete(Long groupChallengeId, Long reactionId, Long currentUserId) {
        Reaction reaction = reactionRepository.findById(reactionId)
                .orElseThrow(() -> new CustomException(ReactionErrorCode.REACTION_NOT_FOUND));

        validateChallengeRecord(groupChallengeId, reaction);

        reaction.deleteBy(currentUserId);
    }

    private void validateChallengeRecord(Long groupChallengeId, Reaction reaction) {
        if (!reaction.getGroupChallengeId().equals(groupChallengeId)) {
            throw new CustomException(ReactionErrorCode.REACTION_CHALLENGE_RECORD_MISMATCH);
        }
    }

    private ReactionResponse toReactionResponse(Reaction reaction) {
        return new ReactionResponse(
                reaction.getId(),
                reaction.getGroupChallengeId(),
                reaction.getActivityRecordId(),
                reaction.getUserId(),
                reaction.getBody().name(),
                reaction.getCreatedAt()
        );
    }
}
