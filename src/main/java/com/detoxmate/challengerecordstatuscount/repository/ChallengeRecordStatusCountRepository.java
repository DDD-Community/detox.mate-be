package com.detoxmate.challengerecordstatuscount.repository;

import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChallengeRecordStatusCountRepository extends JpaRepository<ChallengeRecordStatusCount, Long> {

    @Query("""
            select sc
            from ChallengeRecordStatusCount sc
            where sc.challengeRecordId = :challengeRecordId
            """)
    Optional<ChallengeRecordStatusCount> findByChallengeRecordId(Long challengeRecordId);

}
