package com.detoxmate.challengerecord.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.service.ChallengeRecordStatusCountService;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecord.ChallengeRecordErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ChallengeRecordService {

    private final ChallengeRecordRepository challengeRecordRepository;

    private final ChallengeRecordStatusCountService statusCountService;

    @Transactional(readOnly = true)
    public ChallengeRecord get(Long challengeRecordId) {
        return challengeRecordRepository.findById(challengeRecordId)
                .orElseThrow(() -> new CustomException(
                        ChallengeRecordErrorCode.CHALLENGE_RECORD_NOT_FOUND
                ));
    }

    @Transactional
    public ChallengeRecord create(Long groupChallengeId, Long groupChallengeParticipantId, LocalDate recordDate) {
        ChallengeRecord challengeRecord = challengeRecordRepository
                .findByParticipantDate(groupChallengeId, groupChallengeParticipantId, recordDate)
                .orElseGet(() -> challengeRecordRepository.save(
                        ChallengeRecord.create(groupChallengeId, groupChallengeParticipantId, recordDate)
                ));

        statusCountService.getOrCreate(challengeRecord.getId());

        return challengeRecord;
    }

    @Transactional
    public void certify(Long challengeRecordId, Long activityRecordId, Long activityRecordParticipantId, ChallengeRecordCertificationResult result) {
        ChallengeRecord challengeRecord = challengeRecordRepository.findById(challengeRecordId)
                .orElseThrow(() -> new CustomException(
                        ChallengeRecordErrorCode.CHALLENGE_RECORD_NOT_FOUND
                ));

        challengeRecord.certify(activityRecordId, activityRecordParticipantId, result);
    }

}
