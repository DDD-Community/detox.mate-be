package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.UsageGoalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsageGoalTypeRepository extends JpaRepository<UsageGoalType, Long> {

    Optional<UsageGoalType> findByDescription(String description);
}
