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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityRecordServiceTest {

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository = mock(UserUsageGoalTimeRepository.class);
    private final ActivityRecordService activityRecordService = new ActivityRecordService(userUsageGoalTimeRepository);

    @Test
    void 사용시간이_목표시간_이하이면_detail은_달성이다() {
        // given
        ActivityRecordAchievementCheckRequest request = achievementCheckRequest(
                detailRequest(UsageGoalTypeCode.TOTAL_USAGE, 30)
        );

        when(userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeIn(1L, List.of(UsageGoalTypeCode.TOTAL_USAGE)))
                .thenReturn(List.of(userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 26, 9, 0))));

        // when
        ActivityRecordAchievementCheckResponse response = activityRecordService.checkAchievement(1L, request);

        // then
        assertThat(response.details()).hasSize(1);
        assertThat(response.details().getFirst().goalMinutes()).isEqualTo(60);
        assertThat(response.details().getFirst().isAchieved()).isTrue();
        assertThat(response.allAchieved()).isTrue();
    }

    @Test
    void 같은_타입의_목표시간이_여러개면_가장_최신값을_사용한다() {
        // Given
        ActivityRecordAchievementCheckRequest request = achievementCheckRequest(
                detailRequest(UsageGoalTypeCode.TOTAL_USAGE, 50)
        );

        // When
        when(userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeIn(1L, List.of(UsageGoalTypeCode.TOTAL_USAGE)))
                .thenReturn(List.of(
                        userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 30, LocalDateTime.of(2026, 4, 26, 9, 0)),
                        userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 26, 10, 0))
                ));

        ActivityRecordAchievementCheckResponse response = activityRecordService.checkAchievement(1L, request);

        // Then
        assertThat(response.details()).hasSize(1);
        assertThat(response.details().getFirst().goalMinutes()).isEqualTo(60);
        assertThat(response.details().getFirst().isAchieved()).isTrue();
    }

    @Test
    void 사용가능한_목표시간이_하나도_없으면_400_에러를_반환한다() {
        ActivityRecordAchievementCheckRequest request = achievementCheckRequest(
                detailRequest(UsageGoalTypeCode.TOTAL_USAGE, 50)
        );

        when(userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeIn(1L, List.of(UsageGoalTypeCode.TOTAL_USAGE)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> activityRecordService.checkAchievement(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    private ActivityRecordAchievementCheckRequest achievementCheckRequest(ActivityRecordDetailRequest... details) {
        return new ActivityRecordAchievementCheckRequest(List.of(details));
    }

    private ActivityRecordDetailRequest detailRequest(UsageGoalTypeCode usageGoalTypeCode, Integer usedMinutes) {
        return new ActivityRecordDetailRequest(usageGoalTypeCode, usedMinutes);
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
