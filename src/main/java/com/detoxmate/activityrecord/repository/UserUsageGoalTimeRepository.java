package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface UserUsageGoalTimeRepository extends JpaRepository<UserUsageGoalTime, Long> {
    List<UserUsageGoalTime> findAllByUser_Id(Long userId);

    List<UserUsageGoalTime> findAllByUser_IdIn(Collection<Long> userIds);

    @Query("""
    SELECT COUNT(goal) > 0
    FROM UserUsageGoalTime goal
    WHERE goal.user.id = :userId
""")
    boolean existsGoalByUserId(@Param("userId") Long userId);

    List<UserUsageGoalTime> findAllByUser_IdAndUsageGoalType_CodeIn(Long userId, List<UsageGoalTypeCode> usageGoalTypes);

    List<UserUsageGoalTime> findAllByUser_IdAndUsageGoalType_CodeInAndCreatedAtBefore(
            Long userId,
            List<UsageGoalTypeCode> usageGoalTypes,
            LocalDateTime createdAt
    );
}
