package com.detoxmate.screentimeocr.repository;

import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportAdminListRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScreenTimeOcrErrorReportRepository extends JpaRepository<ScreenTimeOcrErrorReport, Long> {

    @Query(
            value = """
                    select new com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportAdminListRow(
                        report.id,
                        gm.userId,
                        case
                            when u.status = com.detoxmate.user.domain.UserStatus.WITHDRAWN
                            then '탈퇴한 사용자'
                            else u.displayName
                        end,
                        activityRecord.id,
                        participant.id,
                        report.recordDate,
                        report.imageObjectKey,
                        report.ocrTotalUsedMinutes,
                        report.correctedTotalUsedMinutes,
                        report.status,
                        report.adminNote,
                        report.resolvedAt,
                        report.createdAt,
                        report.updatedAt
                    )
                    from ScreenTimeOcrErrorReport report
                    join report.groupChallengeParticipant participant
                    join GroupMember gm on gm.id = participant.groupMemberId
                    left join User u on u.id = gm.userId
                    left join report.activityRecord activityRecord
                    where report.status = :status
                    order by report.createdAt asc, report.id asc
                    """,
            countQuery = """
                    select count(report)
                    from ScreenTimeOcrErrorReport report
                    where report.status = :status
                    """
    )
    Page<ScreenTimeOcrErrorReportAdminListRow> findAdminListRowsByStatus(
            @Param("status") ScreenTimeOcrErrorReportStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"activityRecord", "groupChallengeParticipant"})
    @Query("""
            select report
            from ScreenTimeOcrErrorReport report
            where report.id = :id
            """)
    Optional<ScreenTimeOcrErrorReport> findWithActivityRecordById(@Param("id") Long id);
}
