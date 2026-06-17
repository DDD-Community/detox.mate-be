package com.detoxmate.firstscreentime.repository;

import com.detoxmate.firstscreentime.domain.FirstScreenTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FirstScreenTimeRepository extends JpaRepository<FirstScreenTime, Long> {
    boolean existsByGroupChallengeParticipantId(Long groupChallengeParticipantId);

    Optional<FirstScreenTime> findByGroupChallengeParticipantId(Long groupChallengeParticipantId);
}
