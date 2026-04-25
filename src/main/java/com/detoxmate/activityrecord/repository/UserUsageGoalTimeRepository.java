package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserUsageGoalTimeRepository extends JpaRepository<UserUsageGoalTime, Long> {

    Optional<UserUsageGoalTime> findTopByUserIdAndUsageGoalTypeIdOrderByCreatedAtDesc(Long userId, Long usageGoalTypeId);

    List<UserUsageGoalTime> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
