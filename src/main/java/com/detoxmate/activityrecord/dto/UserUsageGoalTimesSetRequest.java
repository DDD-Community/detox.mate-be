package com.detoxmate.activityrecord.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record UserUsageGoalTimesSetRequest(
        @NotEmpty
        List<@NotNull @Valid UserUsageGoalTimeRequest> goals
) {

    @AssertTrue
    public boolean isUsageGoalTypesUnique() {
        if (goals == null) {
            return true;
        }

        Set<UsageGoalTypeCode> usageGoalTypes = new HashSet<>();
        for (UserUsageGoalTimeRequest goal : goals) {
            if (goal == null || goal.usageGoalType() == null) {
                continue;
            }

            if (!usageGoalTypes.add(goal.usageGoalType())) {
                return false;
            }
        }

        return true;
    }
}
