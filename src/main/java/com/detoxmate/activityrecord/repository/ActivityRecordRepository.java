package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {
}
