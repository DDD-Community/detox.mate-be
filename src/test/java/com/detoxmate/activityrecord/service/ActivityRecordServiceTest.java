package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordAchievementCheckResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateRequest;
import com.detoxmate.activityrecord.dto.ActivityRecordCreateResponse;
import com.detoxmate.activityrecord.dto.ActivityRecordDetailRequest;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ActivityRecordServiceTest {

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository = mock(UserUsageGoalTimeRepository.class);
    private final ActivityRecordRepository activityRecordRepository = mock(ActivityRecordRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository =
            mock(GroupChallengeParticipantRepository.class);
    private final ActivityRecordService activityRecordService =
            new ActivityRecordService(
                    userUsageGoalTimeRepository,
                    activityRecordRepository,
                    userRepository,
                    groupChallengeParticipantRepository
            );

    ActivityRecordServiceTest() {
        ReflectionTestUtils.setField(activityRecordService, "storagePublicBaseUrl", "https://cdn.example.com");
    }

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

    @Test
    void 미달성인_경우_reflectionText가_없으면_400_에러를_반환한다() {
        ActivityRecordCreateRequest request = new ActivityRecordCreateRequest(
                "activity-records/sample.png",
                null,
                10L,
                List.of(detailRequest(UsageGoalTypeCode.TOTAL_USAGE, 80))
        );

        when(userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeIn(1L, List.of(UsageGoalTypeCode.TOTAL_USAGE)))
                .thenReturn(List.of(userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 26, 9, 0))));

        assertThatThrownBy(() -> activityRecordService.create(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void 최종_저장시_detail별_달성여부를_다시_계산한다() {
        ActivityRecordCreateRequest request = new ActivityRecordCreateRequest(
                "activity-records/sample.png",
                "오늘은 산책했다",
                10L,
                List.of(
                        detailRequest(UsageGoalTypeCode.TOTAL_USAGE, 80),
                        detailRequest(UsageGoalTypeCode.INSTAGRAM, 20)
                )
        );

        when(userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeIn(
                1L,
                List.of(UsageGoalTypeCode.TOTAL_USAGE, UsageGoalTypeCode.INSTAGRAM)
        )).thenReturn(List.of(
                userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 26, 9, 0)),
                userUsageGoalTime(UsageGoalTypeCode.INSTAGRAM, 30, LocalDateTime.of(2026, 4, 26, 9, 0))
        ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(groupChallengeParticipantRepository.findById(10L))
                .thenReturn(Optional.of(groupChallengeParticipant(10L)));
        when(activityRecordRepository.save(any(ActivityRecord.class))).thenAnswer(invocation -> {
            ActivityRecord activityRecord = invocation.getArgument(0);
            ReflectionTestUtils.setField(activityRecord, "id", 123L);
            ReflectionTestUtils.setField(activityRecord, "createdAt", LocalDateTime.of(2026, 4, 26, 21, 30));
            return activityRecord;
        });

        activityRecordService.create(1L, request);
        ArgumentCaptor<ActivityRecord> captor = ArgumentCaptor.forClass(ActivityRecord.class);

        verify(activityRecordRepository).save(captor.capture());

        ActivityRecord savedActivityRecord = captor.getValue();
        assertThat(savedActivityRecord.getActivityImageObjectKey()).isEqualTo("activity-records/sample.png");
        assertThat(savedActivityRecord.getReflectionText()).isEqualTo("오늘은 산책했다");
        assertThat(savedActivityRecord.getDetails())
                .extracting(ActivityRecordDetail::getUseMinutes, ActivityRecordDetail::isAchieved)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(80, false),
                        org.assertj.core.groups.Tuple.tuple(20, true)
                );
    }

    @Test
    void 최종_저장시_달성과_미달성이_섞여있으면_allAchieved는_false다() {
        ActivityRecordCreateRequest request = new ActivityRecordCreateRequest(
                "activity-records/sample.png",
                "오늘은 산책했다",
                10L,
                List.of(
                        detailRequest(UsageGoalTypeCode.TOTAL_USAGE, 80),
                        detailRequest(UsageGoalTypeCode.INSTAGRAM, 20)
                )
        );

        when(userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeIn(
                1L,
                List.of(UsageGoalTypeCode.TOTAL_USAGE, UsageGoalTypeCode.INSTAGRAM)
        )).thenReturn(List.of(
                userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 60, LocalDateTime.of(2026, 4, 26, 9, 0)),
                userUsageGoalTime(UsageGoalTypeCode.INSTAGRAM, 30, LocalDateTime.of(2026, 4, 26, 9, 0))
        ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(groupChallengeParticipantRepository.findById(10L))
                .thenReturn(Optional.of(groupChallengeParticipant(10L)));
        when(activityRecordRepository.save(any(ActivityRecord.class))).thenAnswer(invocation -> {
            ActivityRecord activityRecord = invocation.getArgument(0);
            ReflectionTestUtils.setField(activityRecord, "id", 123L);
            ReflectionTestUtils.setField(activityRecord, "createdAt", LocalDateTime.of(2026, 4, 26, 21, 30));
            return activityRecord;
        });

        ActivityRecordCreateResponse response = activityRecordService.create(1L, request);

        assertThat(response.allAchieved()).isFalse();
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
        User user = user(null);
        UsageGoalType usageGoalType = UsageGoalType.create(1L, usageGoalTypeCode);
        UserUsageGoalTime goalTime = UserUsageGoalTime.create(user, usageGoalType, goalMinutes);
        ReflectionTestUtils.setField(goalTime, "createdAt", createdAt);
        return goalTime;
    }

    private User user(Long id) {
        User user = User.createNew("tester");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private GroupChallengeParticipant groupChallengeParticipant(Long id) {
        GroupChallengeParticipant participant = GroupChallengeParticipant.join(100L, 200L);
        ReflectionTestUtils.setField(participant, "id", id);
        return participant;
    }
}
