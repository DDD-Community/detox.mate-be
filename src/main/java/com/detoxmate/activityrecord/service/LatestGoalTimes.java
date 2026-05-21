package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class LatestGoalTimes {

    private final Map<UsageGoalTypeCode, UserUsageGoalTime> values;

    private LatestGoalTimes(Map<UsageGoalTypeCode, UserUsageGoalTime> values) {
        this.values = Map.copyOf(values);
    }

    static LatestGoalTimes from(List<UserUsageGoalTime> goalTimes) {
        Map<UsageGoalTypeCode, UserUsageGoalTime> latestGoalTimes = new HashMap<>();

        for (UserUsageGoalTime goalTime : goalTimes) {
            UsageGoalTypeCode type = goalTime.getUsageGoalType().getCode();
            latestGoalTimes.merge(type, goalTime, LatestGoalTimes::laterGoalTime);
        }

        return new LatestGoalTimes(latestGoalTimes);
    }

    Optional<UserUsageGoalTime> findBy(UsageGoalTypeCode usageGoalType) {
        return Optional.ofNullable(values.get(usageGoalType));
    }

    private static UserUsageGoalTime laterGoalTime(UserUsageGoalTime left, UserUsageGoalTime right) {
        return left.getCreatedAt().isAfter(right.getCreatedAt()) ? left : right;
    }
}
