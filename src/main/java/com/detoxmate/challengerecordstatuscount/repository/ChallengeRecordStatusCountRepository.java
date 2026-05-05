package com.detoxmate.challengerecordstatuscount.repository;

import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChallengeRecordStatusCount sc
            set sc.beforeCommentCount = sc.beforeCommentCount + 1
            where sc.challengeRecordId = :challengeRecordId
            """)
    int increaseBeforeCommentCount(Long challengeRecordId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChallengeRecordStatusCount sc
            set sc.afterCommentCount = sc.afterCommentCount + 1
            where sc.challengeRecordId = :challengeRecordId
            """)
    int increaseAfterCommentCount(Long challengeRecordId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChallengeRecordStatusCount sc
            set sc.reactionCount = sc.reactionCount + 1
            where sc.challengeRecordId = :challengeRecordId
            """)
    int increaseReactionCount(Long challengeRecordId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChallengeRecordStatusCount sc
            set sc.reactionCount =
                case
                    when sc.reactionCount > 0 then sc.reactionCount - 1
                    else 0
                end
            where sc.challengeRecordId = :challengeRecordId
            """)
    int decreaseReactionCount(Long challengeRecordId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChallengeRecordStatusCount sc
            set sc.pokeCount = sc.pokeCount + 1
            where sc.challengeRecordId = :challengeRecordId
            """)
    int increasePokeCount(Long challengeRecordId);

}
