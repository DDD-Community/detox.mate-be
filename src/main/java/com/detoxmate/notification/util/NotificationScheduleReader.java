package com.detoxmate.notification.util;

import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.notification.dto.DailyCertificationReminderTarget;
import com.detoxmate.notification.dto.GoalSettingReminderTarget;
import com.detoxmate.notification.dto.StreakWarningTarget;
import com.detoxmate.notification.dto.WeeklyGoalSummaryTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationScheduleReader {

    private final GroupChallengeParticipantRepository participantRepository;
    private final ChallengeRecordRepository challengeRecordRepository;

    public List<GoalSettingReminderTarget> findGoalSettingReminderTargets(LocalDateTime joinedFrom, LocalDateTime joinedTo) {
        return participantRepository.findGoalSettingReminderTargets(joinedFrom,joinedTo);
    }

    public List<DailyCertificationReminderTarget> findDailyCertificationReminderTargets(LocalDate recordDate) {
        return participantRepository.findDailyCertificationReminderTargets(recordDate);
    }

    public List<StreakWarningTarget> findStreakWarningTargets(LocalDate recordDate) {
        return challengeRecordRepository.findStreakWarningTargets(recordDate);
    }

    public List<WeeklyGoalSummaryTarget> findWeeklyGoalSummaryTargets(LocalDate startDate, LocalDate endDate) {
        return challengeRecordRepository.findWeeklyGoalSummaryTargets(startDate, endDate);
    }
}
