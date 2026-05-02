package com.detoxmate.poke.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.challengerecordstatuscount.service.ChallengeRecordStatusCountService;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.poke.PokeErrorCode;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.repository.PokeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PokeService {

    private final PokeRepository pokeRepository;
    private final ChallengeRecordService challengeRecordService;
    private final ChallengeRecordStatusCountService statusCountService;

    @Transactional
    public void poke(Long challengeRecordId, Long receiverUserId, Long currentUserId) {
        ChallengeRecord challengeRecord = challengeRecordService.get(challengeRecordId);

        validatePokeAllowed(challengeRecord);

        if (pokeRepository.existsPoke(challengeRecordId, currentUserId, receiverUserId)) {
            throw new CustomException(PokeErrorCode.POKE_ALREADY_EXISTS);
        }

        Poke poke = Poke.create(
                challengeRecordId,
                currentUserId,
                receiverUserId,
                LocalDate.now()
        );

        pokeRepository.save(poke);

        statusCountService.increasePokeCount(challengeRecordId);
    }

    @Transactional(readOnly = true)
    public List<Poke> getPokesForChallengeRecord(Long challengeRecordId) {
        return pokeRepository.findAllByChallengeRecordOrderByLatest(challengeRecordId);
    }

    private void validatePokeAllowed(ChallengeRecord challengeRecord) {
        if (challengeRecord.isCertified()) {
            throw new CustomException(PokeErrorCode.POKE_NOT_ALLOWED_AFTER_RECORD);
        }

        if (!challengeRecord.isToday(LocalDate.now())) {
            throw new CustomException(PokeErrorCode.POKE_ONLY_TODAY_ALLOWED);
        }
    }
}
