package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailRequest;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityRecordServiceTest {

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository = mock(UserUsageGoalTimeRepository.class);

    @Test
    void 사용시간이_목표시간_이하이면_detail은_달성이다() {
        // given
        ActivityRecordService activityRecordService = new ActivityRecordService(userUsageGoalTimeRepository);
        ActivityRecordAchievementCheckRequest request = new ActivityRecordAchievementCheckRequest(List.of(
                new ActivityRecordDetailRequest(UsageGoalTypeCode.TOTAL_USAGE, 30)
        ));

        when(userUsageGoalTimeRepository.findTopByUser_IdAndUsageGoalType_CodeOrderByCreatedAtDesc(1L, UsageGoalTypeCode.TOTAL_USAGE))
                .thenReturn(Optional.of(userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 60)));

        // when
        ActivityRecordAchievementCheckResponse response = activityRecordService.checkAchievement(1L, request);

        // then
        assertThat(response.details()).hasSize(1);
        assertThat(response.details().getFirst().goalMinutes()).isEqualTo(60);
        assertThat(response.details().getFirst().isAchieved()).isTrue();
        assertThat(response.allAchieved()).isTrue();
    }

    private UserUsageGoalTime userUsageGoalTime(UsageGoalTypeCode usageGoalTypeCode, Integer goalMinutes) {
        User user = User.createNew("tester");
        UsageGoalType usageGoalType = UsageGoalType.create(1L, usageGoalTypeCode);
        return UserUsageGoalTime.create(user, usageGoalType, goalMinutes);
    }
}
