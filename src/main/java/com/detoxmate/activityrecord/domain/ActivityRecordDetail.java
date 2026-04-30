package com.detoxmate.activityrecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "activity_record_detail",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_ACTIVITY_RECORD_DETAIL_RECORD_GOAL_TYPE",
                columnNames = {"activity_record_id", "usage_goal_type_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityRecordDetail {

    @Id
    @Column(name = "activity_record_detail_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "use_minutes", nullable = false)
    private Integer useMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_record_id", nullable = false)
    private ActivityRecord activityRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usage_goal_type_id", nullable = false)
    private UsageGoalType usageGoalType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_usage_goal_times_id", nullable = false)
    private UserUsageGoalTime userUsageGoalTime;

    @Column(name = "is_achieved", nullable = false)
    private boolean achieved;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ActivityRecordDetail(
            ActivityRecord activityRecord,
            UserUsageGoalTime userUsageGoalTime,
            int useMinutes,
            boolean achieved
    ) {
        validateActivityRecord(activityRecord);
        validateUserUsageGoalTime(userUsageGoalTime);
        validateUseMinutes(useMinutes);
        this.activityRecord = activityRecord;
        this.userUsageGoalTime = userUsageGoalTime;
        this.usageGoalType = userUsageGoalTime.getUsageGoalType();
        this.useMinutes = useMinutes;
        this.achieved = achieved;
    }

    public static ActivityRecordDetail create(
            ActivityRecord activityRecord,
            UserUsageGoalTime userUsageGoalTime,
            int useMinutes,
            boolean achieved
    ) {
        return new ActivityRecordDetail(activityRecord, userUsageGoalTime, useMinutes, achieved);
    }

    private static void validateActivityRecord(ActivityRecord activityRecord) {
        if (activityRecord == null) {
            throw new IllegalArgumentException("activityRecord 는 필수입니다.");
        }
    }

    private static void validateUserUsageGoalTime(UserUsageGoalTime userUsageGoalTime) {
        if (userUsageGoalTime == null) {
            throw new IllegalArgumentException("userUsageGoalTime 는 필수입니다.");
        }
    }

    private static void validateUseMinutes(int useMinutes) {
        if (useMinutes < 0) {
            throw new IllegalArgumentException("usedMinutes 는 0 이상이어야 합니다.");
        }
    }
}
