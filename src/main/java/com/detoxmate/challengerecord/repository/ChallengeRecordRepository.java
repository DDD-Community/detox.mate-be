package com.detoxmate.challengerecord.repository;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.notification.dto.ChallengeRecordNotificationRow;
import com.detoxmate.notification.dto.StreakWarningTarget;
import com.detoxmate.notification.dto.WeeklyGoalSummaryTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRecordRepository extends JpaRepository<ChallengeRecord, Long> {

    @Query("""
            select cr
            from ChallengeRecord cr
            where cr.groupChallengeId = :groupChallengeId
              and cr.groupChallengeParticipantId = :groupChallengeParticipantId
              and cr.recordDate = :recordDate
            """)
    Optional<ChallengeRecord> findByParticipantDate(
            Long groupChallengeId,
            Long groupChallengeParticipantId,
            LocalDate recordDate
    );

    @Query("""
            select cr
            from ChallengeRecord cr
            where cr.groupChallengeId = :groupChallengeId
              and cr.recordDate = :recordDate
            order by cr.id asc
            """)
    List<ChallengeRecord> findAllByGroupChallengeDate(Long groupChallengeId, LocalDate recordDate);

    List<ChallengeRecord> findAllByGroupChallengeIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(
            Long groupChallengeId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<ChallengeRecord> findAllByGroupChallengeParticipantIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(
            Long groupChallengeParticipantId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<ChallengeRecord> findAllByGroupChallengeParticipantIdInAndRecordDateBetweenOrderByRecordDateAscIdAsc(
            List<Long> groupChallengeParticipantIds,
            LocalDate startDate,
            LocalDate endDate
    );

    List<ChallengeRecord> findAllByGroupChallengeParticipantIdInOrderByRecordDateAscIdAsc(
            List<Long> groupChallengeParticipantIds
    );

    @Query("""
            select cr
            from ChallengeRecord cr
            join GroupChallengeParticipant gcp on gcp.id = cr.groupChallengeParticipantId
            join GroupMember gm on gm.id = gcp.groupMemberId
            join User u on u.id = gm.userId
            where cr.groupChallengeId = :groupChallengeId
              and cr.recordDate = :recordDate
            order by u.displayName asc, cr.id asc
            """)
    List<ChallengeRecord> findAllByGroupChallengeDateOrderByDisplayName(Long groupChallengeId, LocalDate recordDate);

    @Query("""
            SELECT new com.detoxmate.notification.dto.ChallengeRecordNotificationRow(
                cr.id,
                cr.groupChallengeId,
                gm.userId,
                u.displayName
            )
            FROM ChallengeRecord cr
            JOIN GroupChallengeParticipant gcp ON gcp.id = cr.groupChallengeParticipantId
            JOIN GroupMember gm ON gm.id = gcp.groupMemberId
            LEFT JOIN User u ON u.id = gm.userId
            WHERE cr.id = :challengeRecordId
              AND gcp.status = 'JOINED'
              AND gm.status = 'ACTIVE'
            """)
    Optional<ChallengeRecordNotificationRow> findChallengeRecordNotificationRow(Long challengeRecordId);

    @Query("""
    SELECT new com.detoxmate.notification.dto.StreakWarningTarget(
        gc.groupId,
        gc.id,
        COUNT(DISTINCT gcp.id),
        COUNT(DISTINCT cr.groupChallengeParticipantId)
    )
    FROM GroupChallenge gc
    JOIN GroupChallengeParticipant gcp ON gcp.groupChallengeId = gc.id
    JOIN GroupMember gm ON gm.id = gcp.groupMemberId
    LEFT JOIN ChallengeRecord cr
        ON cr.groupChallengeParticipantId = gcp.id
       AND cr.recordDate = :recordDate
       AND cr.status IN (
            com.detoxmate.challengerecord.domain.ChallengeRecordStatus.AFTER_RECORD_SUCCESS,
            com.detoxmate.challengerecord.domain.ChallengeRecordStatus.AFTER_RECORD_FAIL
       )
    WHERE gc.status = com.detoxmate.group.domain.GroupChallengeStatus.ACTIVE
      AND gcp.status = 'JOINED'
      AND gm.status = 'ACTIVE'
    GROUP BY gc.groupId, gc.id
""")
    List<StreakWarningTarget> findStreakWarningTargets(@Param("recordDate") LocalDate recordDate);

    @Query("""
    SELECT new com.detoxmate.notification.dto.WeeklyGoalSummaryTarget(
        gm.userId,
        COUNT(cr.id)
    )
    FROM ChallengeRecord cr
    JOIN GroupChallengeParticipant gcp ON gcp.id = cr.groupChallengeParticipantId
    JOIN GroupMember gm ON gm.id = gcp.groupMemberId
    WHERE cr.recordDate BETWEEN :startDate AND :endDate
      AND cr.status = com.detoxmate.challengerecord.domain.ChallengeRecordStatus.AFTER_RECORD_SUCCESS
      AND gcp.status = 'JOINED'
      AND gm.status = 'ACTIVE'
    GROUP BY gm.userId
""")
    List<WeeklyGoalSummaryTarget> findWeeklyGoalSummaryTargets(@Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);


}
