package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsageGoalTypeRepository extends JpaRepository<UsageGoalType, Long> {
    Optional<UsageGoalType> findByCode(UsageGoalTypeCode code);
}
