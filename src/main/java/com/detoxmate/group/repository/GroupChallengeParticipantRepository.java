package com.detoxmate.group.repository;

import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.dto.GroupActivityParticipantRow;
import com.detoxmate.group.dto.GroupChallengeParticipantRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupChallengeParticipantRepository extends JpaRepository<GroupChallengeParticipant, Long> {
    void deleteAllByGroupChallengeIdIn(List<Long> groupChallengeIds);

    Optional<GroupChallengeParticipant> findByGroupChallengeIdAndGroupMemberId(Long groupChallengeId, Long groupMemberId);

    @Query("""
            SELECT new com.detoxmate.group.dto.GroupActivityParticipantRow(
                gcp.groupChallengeId,
                gcp.id,
                gm.id,
                gm.userId,
                u.displayName,
                u.profileImageObjectKey,
                gm.status,
                gm.joinedAt,
                gm.leftAt,
                gcp.status,
                gcp.joinedAt,
                gcp.withdrawnAt
            )
            FROM GroupChallengeParticipant gcp
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            LEFT JOIN User u ON u.id = gm.userId
            WHERE gcp.groupChallengeId = :groupChallengeId
            ORDER BY gm.joinedAt ASC, gcp.joinedAt ASC, gcp.id ASC
            """)
    List<GroupActivityParticipantRow> findActivityParticipantRowsByGroupChallengeId(
            @Param("groupChallengeId") Long groupChallengeId
    );

    @Query("""
            SELECT COUNT(gcp) > 0
            FROM GroupChallengeParticipant gcp
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            WHERE gcp.groupChallengeId = :groupChallengeId
              AND gm.userId = :userId
              AND gcp.status = 'JOINED'
              AND gm.status = 'ACTIVE'
            """)
    boolean existsByGroupChallengeIdAndUserId(@Param("groupChallengeId") Long groupChallengeId,
                                              @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(gcp) > 0
            FROM GroupChallengeParticipant gcp
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            JOIN GroupChallenge gc ON gc.id = gcp.groupChallengeId
            WHERE gcp.id = :participantId
              AND gm.userId = :userId
              AND gcp.status = 'JOINED'
              AND gm.status = 'ACTIVE'
              AND gc.status = 'ACTIVE'
              AND gc.challengeNo = (
                  SELECT MAX(latest.challengeNo)
                  FROM GroupChallenge latest
                  WHERE latest.groupId = gc.groupId
              )
            """)
    boolean existsActiveByIdAndUserId(@Param("participantId") Long participantId,
                                      @Param("userId") Long userId);

    @Query("""
            SELECT new com.detoxmate.group.dto.GroupChallengeParticipantRow(
                gcp.groupChallengeId,
                gcp.id,
                gm.id,
                gm.userId,
                u.displayName,
                u.profileImageObjectKey,
                gcp.status,
                gcp.joinedAt,
                gcp.withdrawnAt,
                CASE WHEN u.status = com.detoxmate.user.domain.UserStatus.WITHDRAWN THEN true ELSE false END
            )
            FROM GroupChallengeParticipant gcp
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            LEFT JOIN User u ON u.id = gm.userId
            WHERE gcp.groupChallengeId = :groupChallengeId
              AND gcp.status = 'JOINED'
              AND gm.status = 'ACTIVE'
            ORDER BY gcp.joinedAt ASC
            """)
    List<GroupChallengeParticipantRow> findParticipantRowsByGroupChallengeId(
            @Param("groupChallengeId") Long groupChallengeId
    );

    @Query("""
            SELECT new com.detoxmate.group.dto.GroupChallengeParticipantRow(
                gcp.groupChallengeId,
                gcp.id,
                gm.id,
                gm.userId,
                u.displayName,
                u.profileImageObjectKey,
                gcp.status,
                gcp.joinedAt,
                gcp.withdrawnAt,
                CASE WHEN u.status = com.detoxmate.user.domain.UserStatus.WITHDRAWN THEN true ELSE false END
            )
            FROM GroupChallengeParticipant gcp
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            LEFT JOIN User u ON u.id = gm.userId
            WHERE gcp.groupChallengeId IN :groupChallengeIds
              AND gcp.status = 'JOINED'
              AND gm.status = 'ACTIVE'
            ORDER BY gcp.groupChallengeId ASC, gcp.joinedAt ASC
            """)
    List<GroupChallengeParticipantRow> findParticipantRowsByGroupChallengeIds(
            @Param("groupChallengeIds") List<Long> groupChallengeIds
    );

    @Query("""
        SELECT new com.detoxmate.group.dto.GroupChallengeParticipantRow(
            gcp.groupChallengeId,
            gcp.id,
            gm.id,
            gm.userId,
            u.displayName,
            u.profileImageObjectKey,
            gcp.status,
            gcp.joinedAt,
            gcp.withdrawnAt,
            CASE WHEN u.status = com.detoxmate.user.domain.UserStatus.WITHDRAWN THEN true ELSE false END
        )
        FROM GroupChallengeParticipant gcp
        JOIN GroupMember gm ON gm.id = gcp.groupMemberId
        LEFT JOIN User u ON u.id = gm.userId
        WHERE gcp.groupChallengeId = :groupChallengeId
                AND gcp.status = 'JOINED'
                AND gm.status = 'ACTIVE'
        ORDER BY u.displayName ASC, gcp.id ASC
        """)
    List<GroupChallengeParticipantRow> findFeedParticipantRowsByGroupChallengeId(
            @Param("groupChallengeId") Long groupChallengeId
    );

    @Query("""
        SELECT new com.detoxmate.group.dto.GroupChallengeParticipantRow(
            gcp.groupChallengeId,
            gcp.id,
            gm.id,
            gm.userId,
            u.displayName,
            u.profileImageObjectKey,
            gcp.status,
            gcp.joinedAt,
            gcp.withdrawnAt,
            CASE WHEN u.status = com.detoxmate.user.domain.UserStatus.WITHDRAWN THEN true ELSE false END
        )
        FROM GroupChallengeParticipant gcp
        JOIN GroupMember gm ON gm.id = gcp.groupMemberId
        LEFT JOIN User u ON u.id = gm.userId
        WHERE gcp.id = :participantId
        """)
    Optional<GroupChallengeParticipantRow> findParticipantRowForFeedDetail(
            @Param("participantId") Long participantId
    );
}
