package com.detoxmate.activityrecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_usage_goal_times")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserUsageGoalTime {

    @Id
    @Column(name = "user_usage_goal_times_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usage_goal_type_id", nullable = false)
    private Long usageGoalTypeId;

    @Column(name = "goal_minutes", nullable = false)
    private Long goalMinutes;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserUsageGoalTime(Long usageGoalTypeId, Long goalMinutes, Long userId) {
        validateGoalMinutes(goalMinutes);
        this.usageGoalTypeId = usageGoalTypeId;
        this.goalMinutes = goalMinutes;
        this.userId = userId;
    }

    public static UserUsageGoalTime create(Long usageGoalTypeId, Long goalMinutes, Long userId) {
        return new UserUsageGoalTime(usageGoalTypeId, goalMinutes, userId);
    }

    private static void validateGoalMinutes(Long goalMinutes) {
        if (goalMinutes == null || goalMinutes < 0) {
            throw new IllegalArgumentException("목표 시간은 0 이상이어야 합니다.");
        }
    }
}
