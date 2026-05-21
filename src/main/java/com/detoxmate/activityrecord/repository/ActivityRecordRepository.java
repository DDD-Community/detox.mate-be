package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {

    @EntityGraph(attributePaths = {
            "user",
            "groupChallengeParticipant",
            "details",
            "details.usageGoalType",
            "details.userUsageGoalTime"
    })
    @Query("""
            select ar
            from ActivityRecord ar
            where ar.id = :activityRecordId
            """)
    Optional<ActivityRecord> findByIdWithDetails(@Param("activityRecordId") Long activityRecordId);
}
