package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserUsageGoalTimeRepository extends JpaRepository<UserUsageGoalTime, Long> {
    List<UserUsageGoalTime> findAllByUser_IdAndUsageGoalType_CodeIn(Long userId, List<UsageGoalTypeCode> usageGoalTypes);
}
