package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivityRecordDetailRepository extends JpaRepository<ActivityRecordDetail, Long> {

    List<ActivityRecordDetail> findAllByActivityRecordIdOrderByCreatedAtAsc(Long activityRecordId);

    Optional<ActivityRecordDetail> findByActivityRecordIdAndUsageGoalTypeId(Long activityRecordId, Long usageGoalTypeId);

    @Query("""
            select ard
            from ActivityRecordDetail ard
            join fetch ard.activityRecord ar
            join ard.usageGoalType ugt
            where ar.id in :activityRecordIds
              and ugt.code = com.detoxmate.activityrecord.dto.UsageGoalTypeCode.TOTAL_USAGE
            order by ar.id asc, ard.id asc
            """)
    List<ActivityRecordDetail> findTotalUsageDetailsByActivityRecordIds(
            @Param("activityRecordIds") List<Long> activityRecordIds
    );
}
