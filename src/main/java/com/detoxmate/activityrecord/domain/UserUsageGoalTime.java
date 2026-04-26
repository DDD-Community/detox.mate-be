package com.detoxmate.activityrecord.domain;

import com.detoxmate.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usage_goal_type_id", nullable = false)
    private UsageGoalType usageGoalType;

    @Column(name = "goal_minutes", nullable = false)
    private Long goalMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserUsageGoalTime(User user, UsageGoalType usageGoalType, long goalMinutes) {
        validateUser(user);
        validateUsageGoalType(usageGoalType);
        validateGoalMinutes(goalMinutes);
        this.user = user;
        this.usageGoalType = usageGoalType;
        this.goalMinutes = goalMinutes;
    }

    public static UserUsageGoalTime create(User user, UsageGoalType usageGoalType, long goalMinutes) {
        return new UserUsageGoalTime(user, usageGoalType, goalMinutes);
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user 는 필수입니다.");
        }
    }

    private static void validateUsageGoalType(UsageGoalType usageGoalType) {
        if (usageGoalType == null) {
            throw new IllegalArgumentException("usageGoalType 는 필수입니다.");
        }
    }

    private static void validateGoalMinutes(long goalMinutes) {
        if (goalMinutes < 0) {
            throw new IllegalArgumentException("goalMinutes 는 0 이상이어야 합니다.");
        }
    }
}
