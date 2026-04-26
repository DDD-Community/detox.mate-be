package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserUsageGoalTimeRepository extends JpaRepository<UserUsageGoalTime, Long> {
    Optional<UserUsageGoalTime> findTopByUser_IdAndUsageGoalType_CodeOrderByCreatedAtDesc(Long userId, UsageGoalTypeCode code);
}
