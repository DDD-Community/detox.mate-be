package com.detoxmate.screentimeocr.repository;

import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScreenTimeOcrErrorReportRepository extends JpaRepository<ScreenTimeOcrErrorReport, Long> {

    @EntityGraph(attributePaths = {"user", "activityRecord"})
    Page<ScreenTimeOcrErrorReport> findAllByStatusOrderByCreatedAtAscIdAsc(
            ScreenTimeOcrErrorReportStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "activityRecord"})
    @Query("""
            select report
            from ScreenTimeOcrErrorReport report
            where report.id = :id
            """)
    Optional<ScreenTimeOcrErrorReport> findWithUserAndActivityRecordById(@Param("id") Long id);
}
