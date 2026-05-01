package com.detoxmate.poke.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.poke.PokeErrorCode;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.repository.PokeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PokeService {

    private final PokeRepository pokeRepository;

    @Transactional
    public void poke(Long groupChallengeId, Long activityRecordId, Long targetUserId, Long currentUserId) {
        LocalDate today = LocalDate.now();

        if (pokeRepository.existsPoke(groupChallengeId, activityRecordId, currentUserId, targetUserId, today)) {
            throw new CustomException(PokeErrorCode.POKE_ALREADY_EXISTS);
        }

        Poke poke = Poke.create(groupChallengeId, activityRecordId, currentUserId, targetUserId, today);

        pokeRepository.save(poke);
    }
}
