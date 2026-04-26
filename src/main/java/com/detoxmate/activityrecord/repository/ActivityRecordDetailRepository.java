package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordDetailRepository extends JpaRepository<ActivityRecordDetail, Long> {
}
