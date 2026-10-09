package com.detoxmate.activityrecord.domain;

import com.detoxmate.support.UserFixtures;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserUsageGoalTimeTest {

    @Test
    void 목표시간은_null일_수_없다() {
        assertThatThrownBy(() -> UserUsageGoalTime.create(
                UserFixtures.createUser("tester"),
                UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE),
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
