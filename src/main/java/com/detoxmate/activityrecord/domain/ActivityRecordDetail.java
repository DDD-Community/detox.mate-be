package com.detoxmate.activityrecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_ACTIVITY_RECORD_DETAIL_RECORD_GOAL_TYPE",
                        columnNames = {"activity_record_id", "usage_goal_type_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityRecordDetail {

    @Id
    @Column(name = "activity_record_detail_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_record_id", nullable = false)
    private Long activityRecordId;

    @Column(name = "usage_goal_type_id", nullable = false)
    private Long usageGoalTypeId;

    @Column(name = "user_usage_goal_times_id", nullable = false)
    private Long userUsageGoalTimesId;

    @Column(name = "use_minutes", nullable = false)
    private Integer useMinutes;

    @Column(name = "is_achieved", nullable = false)
    private boolean achieved;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ActivityRecordDetail(
            Long activityRecordId,
            Long usageGoalTypeId,
            Long userUsageGoalTimesId,
            Integer useMinutes,
            boolean achieved
    ) {
        validateUseMinutes(useMinutes);
        this.activityRecordId = activityRecordId;
        this.usageGoalTypeId = usageGoalTypeId;
        this.userUsageGoalTimesId = userUsageGoalTimesId;
        this.useMinutes = useMinutes;
        this.achieved = achieved;
    }

    public static ActivityRecordDetail create(
            Long activityRecordId,
            Long usageGoalTypeId,
            Long userUsageGoalTimesId,
            Integer useMinutes,
            boolean achieved
    ) {
        return new ActivityRecordDetail(activityRecordId, usageGoalTypeId, userUsageGoalTimesId, useMinutes, achieved);
    }

    private static void validateUseMinutes(Integer useMinutes) {
        if (useMinutes == null || useMinutes < 0) {
            throw new IllegalArgumentException("사용 시간은 0 이상이어야 합니다.");
        }
    }
}
