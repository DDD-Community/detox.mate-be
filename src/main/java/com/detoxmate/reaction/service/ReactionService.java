package com.detoxmate.reaction.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.challengerecordstatuscount.service.ChallengeRecordStatusCountService;
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
    private final ChallengeRecordService challengeRecordService;
    private final ChallengeRecordStatusCountService statusCountService;

    @Transactional
    public ReactionResponse create(Long challengeRecordId, CreateReactionRequest request, Long currentUserId) {
        ChallengeRecord challengeRecord = challengeRecordService.get(challengeRecordId);
        validateReactionAllowed(challengeRecord);

        ReactionBody body = ReactionBody.valueOf(request.reactionCode());

        if (reactionRepository.existsActiveReaction(challengeRecordId, currentUserId, body)) {
            throw new CustomException(ReactionErrorCode.REACTION_ALREADY_EXISTS);
        }

        Reaction reaction = Reaction.create(challengeRecordId, currentUserId, body);
        Reaction saved = reactionRepository.save(reaction);

        statusCountService.increaseReactionCount(challengeRecordId);

        return toReactionResponse(saved);
    }

    @Transactional
    public void delete(Long challengeRecordId, Long reactionId, Long currentUserId) {
        Reaction reaction = reactionRepository.findById(reactionId)
                .orElseThrow(() -> new CustomException(ReactionErrorCode.REACTION_NOT_FOUND));

        validateChallengeRecord(challengeRecordId, reaction);

        reaction.deleteBy(currentUserId);
    }

    private void validateReactionAllowed(ChallengeRecord challengeRecord) {
        if (!challengeRecord.isCertified()) {
            throw new CustomException(ReactionErrorCode.REACTION_NOT_ALLOWED_BEFORE_RECORD);
        }
    }

    private void validateChallengeRecord(Long challengeRecordId, Reaction reaction) {
        if (!reaction.getChallengeRecordId().equals(challengeRecordId)) {
            throw new CustomException(ReactionErrorCode.REACTION_CHALLENGE_RECORD_MISMATCH);
        }
    }

    private ReactionResponse toReactionResponse(Reaction reaction) {
        return new ReactionResponse(
                reaction.getId(),
                reaction.getChallengeRecordId(),
                reaction.getUserId(),
                reaction.getBody().name(),
                reaction.getCreatedAt()
        );
    }
}
