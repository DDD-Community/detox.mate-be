package com.detoxmate.notification.scheduler;

import com.detoxmate.notification.domain.NotificationSourceType;
import com.detoxmate.notification.domain.NotificationTargetType;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.dto.StreakWarningTarget;
import com.detoxmate.notification.dto.WeeklyGoalSummaryTarget;
import com.detoxmate.notification.service.NotificationCommand;
import com.detoxmate.notification.service.NotificationService;
import com.detoxmate.notification.util.NotificationRecipientReader;
import com.detoxmate.notification.util.NotificationScheduleReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-18T14:30:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private NotificationScheduleReader scheduleReader;

    @Mock
    private NotificationRecipientReader recipientReader;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void 스트릭_유지_알림은_그룹_멤버에게_피드_이동_payload로_발송된다() {
        NotificationScheduler scheduler = scheduler();
        Long groupId = 10L;
        Long groupChallengeId = 20L;
        when(scheduleReader.findStreakWarningTargets(java.time.LocalDate.of(2026, 5, 18)))
                .thenReturn(List.of(new StreakWarningTarget(groupId, groupChallengeId, 3, 1)));
        when(recipientReader.findActiveGroupMemberUserIds(groupId)).thenReturn(List.of(1L, 2L));

        scheduler.sendStreakWarnings();

        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService, org.mockito.Mockito.times(2)).send(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(NotificationCommand::recipientUserId)
                .containsExactly(1L, 2L);
        assertThat(captor.getAllValues()).allSatisfy(command -> {
            assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.STREAK_WARNING);
            assertThat(command.context().get("remainingCount")).isEqualTo("1");
            assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.FEED);
            assertThat(command.payload().targetId()).isEqualTo(groupChallengeId);
            assertThat(command.payload().sourceType()).isEqualTo(NotificationSourceType.NONE);
            assertThat(command.payload().sourceId()).isNull();
        });
    }

    @Test
    void 스트릭_유지_조건을_이미_채웠으면_알림을_발송하지_않는다() {
        NotificationScheduler scheduler = scheduler();
        when(scheduleReader.findStreakWarningTargets(java.time.LocalDate.of(2026, 5, 18)))
                .thenReturn(List.of(new StreakWarningTarget(10L, 20L, 3, 2)));

        scheduler.sendStreakWarnings();

        verify(notificationService, never()).send(org.mockito.ArgumentMatchers.any(NotificationCommand.class));
    }

    @Test
    void 주간_목표_요약_알림은_마이페이지_이동_payload로_발송된다() {
        NotificationScheduler scheduler = scheduler();
        when(scheduleReader.findWeeklyGoalSummaryTargets(
                java.time.LocalDate.of(2026, 5, 10),
                java.time.LocalDate.of(2026, 5, 16)
        )).thenReturn(List.of(new WeeklyGoalSummaryTarget(1L, 4)));

        scheduler.sendWeeklyGoalSummaries();

        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService).send(captor.capture());

        NotificationCommand command = captor.getValue();
        assertThat(command.recipientUserId()).isEqualTo(1L);
        assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.WEEKLY_GOAL_SUMMARY);
        assertThat(command.context().get("achievementCount")).isEqualTo("4");
        assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.MY_PAGE);
        assertThat(command.payload().targetId()).isNull();
        assertThat(command.payload().sourceType()).isEqualTo(NotificationSourceType.NONE);
        assertThat(command.payload().sourceId()).isNull();
    }

    private NotificationScheduler scheduler() {
        return new NotificationScheduler(
                scheduleReader,
                recipientReader,
                notificationService,
                eventPublisher,
                FIXED_CLOCK
        );
    }
}
