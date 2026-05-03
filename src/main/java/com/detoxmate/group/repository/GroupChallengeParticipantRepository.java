package com.detoxmate.group.repository;

import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.dto.GroupChallengeParticipantRow;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupChallengeParticipantRepository extends JpaRepository<GroupChallengeParticipant, Long> {
    void deleteAllByGroupChallengeIdIn(List<Long> groupChallengeIds);

    Optional<GroupChallengeParticipant> findByGroupChallengeIdAndGroupMemberId(Long groupChallengeId, Long groupMemberId);

    @Query("""
            SELECT COUNT(gcp) > 0
            FROM GroupChallengeParticipant gcp
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            WHERE gcp.groupChallengeId = :groupChallengeId
              AND gm.userId = :userId
            """)
    boolean existsByGroupChallengeIdAndUserId(
            @Param("groupChallengeId") Long groupChallengeId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT new com.detoxmate.group.dto.GroupChallengeParticipantResponse(
                gcp.id,
                gm.id,
                gm.userId,
                u.displayName,
                u.profileImageUrl,
                gcp.status,
                gcp.joinedAt,
                gcp.withdrawnAt,
                null
            )
            FROM GroupChallengeParticipant gcp
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            LEFT JOIN User u ON u.id = gm.userId
            WHERE gcp.groupChallengeId = :groupChallengeId
            ORDER BY gcp.joinedAt ASC
            """)
    List<GroupChallengeParticipantResponse> findParticipantResponsesByGroupChallengeId(
            @Param("groupChallengeId") Long groupChallengeId
    );

    @Query("""
            SELECT new com.detoxmate.group.dto.GroupChallengeParticipantRow(
                gcp.groupChallengeId,
                gcp.id,
                gm.id,
                gm.userId,
                u.displayName,
                u.profileImageUrl,
                gcp.status,
                gcp.joinedAt,
                gcp.withdrawnAt
            )
            FROM GroupChallengeParticipant gcp
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            LEFT JOIN User u ON u.id = gm.userId
            WHERE gcp.groupChallengeId IN :groupChallengeIds
            ORDER BY gcp.groupChallengeId ASC, gcp.joinedAt ASC
            """)
    List<GroupChallengeParticipantRow> findParticipantRowsByGroupChallengeIds(
            @Param("groupChallengeIds") List<Long> groupChallengeIds
    );

    @Query("""
        SELECT new com.detoxmate.group.dto.GroupChallengeParticipantResponse(
            gcp.id,
            gm.id,
            gm.userId,
            u.displayName,
            u.profileImageUrl,
            gcp.status,
            gcp.joinedAt,
            gcp.withdrawnAt,
            null
        )
        FROM GroupChallengeParticipant gcp
        JOIN GroupMember gm ON gm.id = gcp.groupMemberId
        LEFT JOIN User u ON u.id = gm.userId
        WHERE gcp.groupChallengeId = :groupChallengeId
        ORDER BY u.displayName ASC, gcp.id ASC
        """)
    List<GroupChallengeParticipantResponse> findFeedParticipantResponsesByGroupChallengeId(
            @Param("groupChallengeId") Long groupChallengeId
    );

    @Query("""
        SELECT new com.detoxmate.group.dto.GroupChallengeParticipantResponse(
            gcp.id,
            gm.id,
            gm.userId,
            u.displayName,
            u.profileImageUrl,
            gcp.status,
            gcp.joinedAt,
            gcp.withdrawnAt,
            null
        )
        FROM GroupChallengeParticipant gcp
        JOIN GroupMember gm ON gm.id = gcp.groupMemberId
        LEFT JOIN User u ON u.id = gm.userId
        WHERE gcp.id = :participantId
        """)
    Optional<GroupChallengeParticipantResponse> findParticipantResponseForFeedDetail(
            @Param("participantId") Long participantId
    );
}
