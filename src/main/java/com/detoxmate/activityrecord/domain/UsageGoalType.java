package com.detoxmate.activityrecord.domain;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "description", nullable = false, length = 50)
    private UsageGoalTypeCode code;

    private UsageGoalType(Long id, UsageGoalTypeCode code) {
        validateId(id);
        validateCode(code);
        this.id = id;
        this.code = code;
    }

    public static UsageGoalType create(Long id, UsageGoalTypeCode code) {
        return new UsageGoalType(id, code);
    }

    private static void validateId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("usageGoalTypeId 는 필수입니다.");
        }
    }

    private static void validateCode(UsageGoalTypeCode code) {
        if (code == null) {
            throw new IllegalArgumentException("usageGoalTypeCode 는 필수입니다.");
        }
    }
}
