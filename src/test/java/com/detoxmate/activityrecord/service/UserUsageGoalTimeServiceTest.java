package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.CurrentUsageGoalTimesResponse;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimeRequest;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimesSetRequest;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimesSetResponse;
import com.detoxmate.activityrecord.repository.UsageGoalTypeRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserUsageGoalTimeServiceTest {

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository = mock(UserUsageGoalTimeRepository.class);
    private final UsageGoalTypeRepository usageGoalTypeRepository = mock(UsageGoalTypeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserUsageGoalTimeService userUsageGoalTimeService = new UserUsageGoalTimeService(
            userUsageGoalTimeRepository,
            usageGoalTypeRepository,
            userRepository
    );

    @Test
    @SuppressWarnings("unchecked")
    void 목표시간_설정_요청_개수만큼_사용자_목표시간_이력을_추가한다() {
        User user = user(1L);
        UsageGoalType totalUsage = usageGoalType(1L, UsageGoalTypeCode.TOTAL_USAGE);
        UsageGoalType instagram = usageGoalType(2L, UsageGoalTypeCode.INSTAGRAM);
        UserUsageGoalTimesSetRequest request = new UserUsageGoalTimesSetRequest(List.of(
                new UserUsageGoalTimeRequest(UsageGoalTypeCode.TOTAL_USAGE, 60),
                new UserUsageGoalTimeRequest(UsageGoalTypeCode.INSTAGRAM, 30)
        ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(usageGoalTypeRepository.findByCode(UsageGoalTypeCode.TOTAL_USAGE)).thenReturn(Optional.of(totalUsage));
        when(usageGoalTypeRepository.findByCode(UsageGoalTypeCode.INSTAGRAM)).thenReturn(Optional.of(instagram));
        when(userUsageGoalTimeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> {
            List<UserUsageGoalTime> goalTimes = invocation.getArgument(0);
            setPersistenceFields(goalTimes.get(0), 101L, LocalDateTime.of(2026, 4, 29, 10, 30));
            setPersistenceFields(goalTimes.get(1), 102L, LocalDateTime.of(2026, 4, 29, 10, 30));
            return goalTimes;
        });

        UserUsageGoalTimesSetResponse response = userUsageGoalTimeService.setGoalTimes(1L, request);

        ArgumentCaptor<Iterable<UserUsageGoalTime>> captor = ArgumentCaptor.forClass(Iterable.class);
        org.mockito.Mockito.verify(userUsageGoalTimeRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue())
                .extracting(UserUsageGoalTime::getUser, goalTime -> goalTime.getUsageGoalType().getCode(), UserUsageGoalTime::getGoalMinutes)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(user, UsageGoalTypeCode.TOTAL_USAGE, 60),
                        org.assertj.core.groups.Tuple.tuple(user, UsageGoalTypeCode.INSTAGRAM, 30)
                );
        assertThat(response.goals())
                .extracting("id", "usageGoalType", "goalMinutes", "createdAt")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 29, 10, 30)),
                        org.assertj.core.groups.Tuple.tuple(102L, UsageGoalTypeCode.INSTAGRAM, 30, LocalDateTime.of(2026, 4, 29, 10, 30))
                );
    }

    @Test
    void 현재_목표시간은_목표타입별_최신_이력만_반환한다() {
        when(userUsageGoalTimeRepository.findAllByUser_Id(1L)).thenReturn(List.of(
                userUsageGoalTime(101L, UsageGoalTypeCode.TOTAL_USAGE, 30, LocalDateTime.of(2026, 4, 28, 9, 0)),
                userUsageGoalTime(102L, UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 29, 9, 0)),
                userUsageGoalTime(103L, UsageGoalTypeCode.INSTAGRAM, 20, LocalDateTime.of(2026, 4, 29, 10, 0))
        ));

        CurrentUsageGoalTimesResponse response = userUsageGoalTimeService.getCurrentGoalTimes(1L);

        assertThat(response.goals())
                .extracting("id", "usageGoalType", "goalMinutes", "createdAt")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(102L, UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 29, 9, 0)),
                        org.assertj.core.groups.Tuple.tuple(103L, UsageGoalTypeCode.INSTAGRAM, 20, LocalDateTime.of(2026, 4, 29, 10, 0))
                );
    }

    @Test
    void 사용가능하지_않은_목표타입이면_400_에러를_반환한다() {
        UserUsageGoalTimesSetRequest request = new UserUsageGoalTimesSetRequest(List.of(
                new UserUsageGoalTimeRequest(UsageGoalTypeCode.TOTAL_USAGE, 60)
        ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(usageGoalTypeRepository.findByCode(UsageGoalTypeCode.TOTAL_USAGE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userUsageGoalTimeService.setGoalTimes(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(400);
    }

    private UserUsageGoalTime userUsageGoalTime(
            Long id,
            UsageGoalTypeCode usageGoalTypeCode,
            Integer goalMinutes,
            LocalDateTime createdAt
    ) {
        UserUsageGoalTime goalTime = UserUsageGoalTime.create(user(1L), usageGoalType(1L, usageGoalTypeCode), goalMinutes);
        setPersistenceFields(goalTime, id, createdAt);
        return goalTime;
    }

    private User user(Long id) {
        User user = User.createNew("tester");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UsageGoalType usageGoalType(Long id, UsageGoalTypeCode code) {
        return UsageGoalType.create(id, code);
    }

    private void setPersistenceFields(UserUsageGoalTime goalTime, Long id, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(goalTime, "id", id);
        ReflectionTestUtils.setField(goalTime, "createdAt", createdAt);
    }
}
