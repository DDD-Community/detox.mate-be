package com.detoxmate.challengerecordstatuscount.service;


import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecordstatuscount.ChallengeRecordStatusCountErrorCode;
import lombok.RequiredArgsConstructor;
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
    public void increaseBeforeCommentCount(Long challengeRecordId) {
        ChallengeRecordStatusCount statusCount = getStatusCount(challengeRecordId);

        statusCount.increaseBeforeCommentCount();
    }

    @Transactional
    public void increaseAfterCommentCount(Long challengeRecordId) {
        ChallengeRecordStatusCount statusCount = getStatusCount(challengeRecordId);

        statusCount.increaseAfterCommentCount();
    }

    @Transactional
    public void increaseReactionCount(Long challengeRecordId) {
        ChallengeRecordStatusCount statusCount = getStatusCount(challengeRecordId);

        statusCount.increaseReactionCount();
    }

    @Transactional
    public void increasePokeCount(Long challengeRecordId) {
        ChallengeRecordStatusCount statusCount = getStatusCount(challengeRecordId);

        statusCount.increasePokeCount();
    }

    private ChallengeRecordStatusCount getStatusCount(Long challengeRecordId) {
        return statusCountRepository.findByChallengeRecordId(challengeRecordId)
                .orElseThrow(() -> new CustomException(
                        ChallengeRecordStatusCountErrorCode.CHALLENGE_RECORD_STATUS_COUNT_NOT_FOUND
                ));
    }

}
