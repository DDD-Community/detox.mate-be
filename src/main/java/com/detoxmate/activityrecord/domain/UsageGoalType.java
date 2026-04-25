package com.detoxmate.activityrecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "usage_goal_type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageGoalType {

    @Id
    @Column(name = "usage_goal_type_id")
    private Long id;

    @Column(name = "description", nullable = false, length = 50)
    private String description;

    private UsageGoalType(Long id, String description) {
        this.id = id;
        this.description = description;
    }

    public static UsageGoalType of(Long id, String description) {
        return new UsageGoalType(id, description);
    }
}
