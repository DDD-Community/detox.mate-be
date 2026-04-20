package com.detoxmate.group.repository;

import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupChallengeRepository extends JpaRepository<GroupChallenge, Long> {
    Optional<GroupChallenge> findTopByGroupIdOrderByChallengeNoDesc(Long groupId);

    @Query("""
            SELECT DISTINCT gc
            FROM GroupChallenge gc
            JOIN GroupChallengeParticipant gcp ON gcp.groupChallengeId = gc.id
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            WHERE gm.userId = :userId
              AND (:status IS NULL OR gc.status = :status)
            ORDER BY gc.createdAt DESC
            """)
    List<GroupChallenge> findAllByParticipantUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") GroupChallengeStatus status
    );
}
