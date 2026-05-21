package com.detoxmate.challengerecordstatuscount.service;


import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecordstatuscount.ChallengeRecordStatusCountErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChallengeRecordStatusCountService {

    private final ChallengeRecordStatusCountRepository statusCountRepository;

    @Transactional
    public ChallengeRecordStatusCount create(Long challengeRecordId) {
        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(challengeRecordId);

        return statusCountRepository.save(statusCount);
    }

    @Transactional
    public ChallengeRecordStatusCount getOrCreate(Long challengeRecordId) {
        return statusCountRepository.findByChallengeRecordId(challengeRecordId)
                .orElseGet(() -> createOrFindExisting(challengeRecordId));
    }

    private ChallengeRecordStatusCount createOrFindExisting(Long challengeRecordId) {
        try {
            return statusCountRepository.saveAndFlush(ChallengeRecordStatusCount.create(challengeRecordId));
        } catch (DataIntegrityViolationException exception) {
            return statusCountRepository.findByChallengeRecordId(challengeRecordId)
                    .orElseThrow(() -> exception);
        }
    }

    @Transactional
    public void increaseBeforeCommentCount(Long challengeRecordId) {
        validateUpdated(statusCountRepository.increaseBeforeCommentCount(challengeRecordId));
    }

    @Transactional
    public void increaseAfterCommentCount(Long challengeRecordId) {
        validateUpdated(statusCountRepository.increaseAfterCommentCount(challengeRecordId));
    }

    @Transactional
    public void increaseReactionCount(Long challengeRecordId) {
        validateUpdated(statusCountRepository.increaseReactionCount(challengeRecordId));
    }

    @Transactional
    public void decreaseReactionCount(Long challengeRecordId) {
        validateUpdated(statusCountRepository.decreaseReactionCount(challengeRecordId));
    }

    @Transactional
    public void increasePokeCount(Long challengeRecordId) {
        validateUpdated(statusCountRepository.increasePokeCount(challengeRecordId));
    }

    private void validateUpdated(int updatedCount) {
        if (updatedCount == 0) {
            throw new CustomException(
                    ChallengeRecordStatusCountErrorCode.CHALLENGE_RECORD_STATUS_COUNT_NOT_FOUND
            );
        }
    }

}
