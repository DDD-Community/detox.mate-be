package com.detoxmate.group.repository;

import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.dto.GroupActivityParticipantRow;
import com.detoxmate.group.dto.GroupChallengeParticipantRow;
import com.detoxmate.notification.dto.DailyCertificationReminderTarget;
import com.detoxmate.notification.dto.GoalSettingReminderTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GroupChallengeParticipantRepository extends JpaRepository<GroupChallengeParticipant, Long> {
    void deleteAllByGroupChallengeIdIn(List<Long> groupChallengeIds);

    Optional<GroupChallengeParticipant> findByGroupChallengeIdAndGroupMemberId(Long groupChallengeId, Long groupMemberId);

    List<GroupChallengeParticipant> findAllByGroupMemberIdOrderByJoinedAtAscIdAsc(Long groupMemberId);

    @Query("""
            SELECT new com.detoxmate.group.dto.GroupActivityParticipantRow(
                gcp.groupChallengeId,
                gcp.id,
                gm.id,
                gm.userId,
                u.displayName,
                u.profileImageObjectKey,
                CASE WHEN u.status = com.detoxmate.user.domain.UserStatus.WITHDRAWN THEN true ELSE false END,
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

    @Query("""
    SELECT new com.detoxmate.notification.dto.GoalSettingReminderTarget(
        gc.groupId,
        gm.userId
    )
    FROM GroupChallengeParticipant gcp
    JOIN GroupMember gm ON gm.id = gcp.groupMemberId
    JOIN GroupChallenge gc ON gc.id = gcp.groupChallengeId
    WHERE gcp.status = 'JOINED'
      AND gm.status = 'ACTIVE'
      AND gcp.joinedAt >= :joinedFrom
      AND gcp.joinedAt < :joinedTo
      AND NOT EXISTS (
          SELECT 1
          FROM UserUsageGoalTime goal
          WHERE goal.user.id = gm.userId
      )
      AND NOT EXISTS (
          SELECT 1
          FROM NotificationHistory history
          JOIN history.notification notification
          JOIN notification.type type
          WHERE history.userId = gm.userId
            AND type.typeCode = com.detoxmate.notification.domain.NotificationTypeCode.GOAL_SETTING_REMINDER
            AND history.createdAt >= gcp.joinedAt
      )
""")
    List<GoalSettingReminderTarget> findGoalSettingReminderTargets(@Param("joinedFrom") LocalDateTime joinedFrom,
                                                                   @Param("joinedTo") LocalDateTime joinedTo);



    @Query("""
    SELECT new com.detoxmate.notification.dto.DailyCertificationReminderTarget(
        gm.userId,
        gcp.groupChallengeId
    )
    FROM GroupChallengeParticipant gcp
    JOIN GroupMember gm ON gm.id = gcp.groupMemberId
    JOIN GroupChallenge gc ON gc.id = gcp.groupChallengeId
    WHERE gcp.status = 'JOINED'
      AND gm.status = 'ACTIVE'
      AND gc.status = com.detoxmate.group.domain.GroupChallengeStatus.ACTIVE
      AND NOT EXISTS (
          SELECT 1
          FROM ChallengeRecord cr
          WHERE cr.groupChallengeParticipantId = gcp.id
            AND cr.recordDate = :recordDate
            AND cr.status IN (
                com.detoxmate.challengerecord.domain.ChallengeRecordStatus.AFTER_RECORD_SUCCESS,
                com.detoxmate.challengerecord.domain.ChallengeRecordStatus.AFTER_RECORD_FAIL
            )
      )
""")
    List<DailyCertificationReminderTarget> findDailyCertificationReminderTargets(
            @Param("recordDate") LocalDate recordDate
    );


}
