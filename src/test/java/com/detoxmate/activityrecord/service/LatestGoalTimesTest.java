package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LatestGoalTimesTest {
    @Test
    void 목표시간에서_같은_타입의_목표가_여러개이면_가장_최신의_목표만_반환한다() {
        LatestGoalTimes latestGoalTimes = LatestGoalTimes.from(List.of(
                userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 30, LocalDateTime.of(2026, 4, 26, 9, 0)),
                userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 26, 10, 0))
        ));

        assertThat(latestGoalTimes.findBy(UsageGoalTypeCode.TOTAL_USAGE))
                .isPresent()
                .get()
                .extracting(UserUsageGoalTime::getGoalMinutes)
                .isEqualTo(60);
    }

    private UserUsageGoalTime userUsageGoalTime(
            UsageGoalTypeCode usageGoalTypeCode,
            Integer goalMinutes,
            LocalDateTime createdAt
    ) {
        User user = User.createNew("tester");
        UsageGoalType usageGoalType = UsageGoalType.create(1L, usageGoalTypeCode);
        UserUsageGoalTime goalTime = UserUsageGoalTime.create(user, usageGoalType, goalMinutes);
        ReflectionTestUtils.setField(goalTime, "createdAt", createdAt);
        return goalTime;
    }
}
