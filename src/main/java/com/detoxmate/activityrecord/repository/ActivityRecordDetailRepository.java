package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRecordDetailRepository extends JpaRepository<ActivityRecordDetail, Long> {

    List<ActivityRecordDetail> findAllByActivityRecordIdOrderByCreatedAtAsc(Long activityRecordId);

    Optional<ActivityRecordDetail> findByActivityRecordIdAndUsageGoalTypeId(Long activityRecordId, Long usageGoalTypeId);
}
