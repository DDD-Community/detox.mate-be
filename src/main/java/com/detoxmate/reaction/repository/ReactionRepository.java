package com.detoxmate.reaction.repository;

import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.domain.ReactionBody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    @Query("""
            select r
            from Reaction r
            where r.groupChallengeId = :groupChallengeId
              and r.activityRecordId = :activityRecordId
              and r.deleted = false
            order by r.id asc
            """)
    List<Reaction> findActiveByChallengeRecord(Long groupChallengeId, Long activityRecordId);

    @Query("""
            select count(r)
            from Reaction r
            where r.groupChallengeId = :groupChallengeId
              and r.activityRecordId = :activityRecordId
              and r.deleted = false
            """)
    long countActiveByChallengeRecord(Long groupChallengeId, Long activityRecordId);

    @Query("""
            select count(r) > 0
            from Reaction r
            where r.groupChallengeId = :groupChallengeId
              and r.activityRecordId = :activityRecordId
              and r.userId = :userId
              and r.body = :body
              and r.deleted = false
            """)
    boolean existsActiveReaction(Long groupChallengeId, Long activityRecordId, Long userId, ReactionBody body);
}
