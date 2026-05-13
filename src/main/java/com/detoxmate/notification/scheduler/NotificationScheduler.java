package com.detoxmate.notification.scheduler;

import com.detoxmate.notification.domain.NotificationContext;
import com.detoxmate.notification.domain.NotificationPayload;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.event.GoalSettingReminderEvent;
import com.detoxmate.notification.service.NotificationCommand;
import com.detoxmate.notification.service.NotificationService;
import com.detoxmate.notification.util.NotificationRecipientReader;
import com.detoxmate.notification.util.NotificationScheduleReader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NotificationScheduleReader scheduleReader;
    private final NotificationRecipientReader recipientReader;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Seoul")
    public void sendGoalSettingReminders() {
        LocalDate joinedDate = LocalDate.now(clock.withZone(KST)).minusDays(1);

        LocalDateTime joinedFrom = joinedDate.atStartOfDay();
        LocalDateTime joinedTo = joinedDate.plusDays(1).atStartOfDay();

        scheduleReader.findGoalSettingReminderTargets(joinedFrom, joinedTo)
                .forEach(target -> eventPublisher.publishEvent(
                        new GoalSettingReminderEvent(target.groupId(), target.userId())
                ));
    }


    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
    public void sendDailyCertificationReminders(){
        LocalDate today = LocalDate.now(clock.withZone(KST));

        scheduleReader.findDailyCertificationReminderTargets(today)
                .forEach(target->notificationService.send(NotificationCommand.history(
                        target.userId(),
                        NotificationTypeCode.DAILY_CERTIFICATION_REMINDER,
                        NotificationContext.empty(),
                        NotificationPayload.feed(target.groupChallengeId())
                )));

    }

    @Scheduled(cron = "0 30 23 * * *",zone = "Asia/Seoul")
    public void sendStreakWarnings(){
        LocalDate today = LocalDate.now(clock.withZone(KST));

        scheduleReader.findStreakWarningTargets(today)
                .forEach(target -> {
                    long requiredCount = requiredCertificationCount(target.participantCount());
                    long remainingCount = requiredCount - target.certifiedCount();

                    if (remainingCount <= 0) {
                        return;
                    }

                    recipientReader.findActiveGroupMemberUserIds(target.groupId())
                            .forEach(userId -> notificationService.send(NotificationCommand.history(
                                    userId,
                                    NotificationTypeCode.STREAK_WARNING,
                                    NotificationContext.of("remainingCount", String.valueOf(remainingCount)),
                                    NotificationPayload.group(target.groupId())
                            )));
                });
    }

    @Scheduled(cron = "0 0 10 * * MON", zone = "Asia/Seoul")
    public void sendWeeklyGoalSummaries() {
        LocalDate today = LocalDate.now(clock.withZone(KST));
        LocalDate startDate = today.minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        LocalDate endDate = startDate.plusDays(6);

        scheduleReader.findWeeklyGoalSummaryTargets(startDate, endDate)
                .forEach(target -> notificationService.send(NotificationCommand.history(
                        target.userId(),
                        NotificationTypeCode.WEEKLY_GOAL_SUMMARY,
                        NotificationContext.of("achievementCount", String.valueOf(target.achievementCount())),
                        NotificationPayload.none()
                )));
    }

    private long requiredCertificationCount(long participantCount) {
        return (participantCount + 1) / 2;
    }
}
