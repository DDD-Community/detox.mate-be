package com.detoxmate.group.activity;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GroupActivityVerificationPolicy {

    public LocalDate firstVerificationDate(
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            LocalDate today
    ) {
        if (participants.isEmpty() || goals.isEmpty()) {
            return null;
        }

        LocalDate firstCandidate = firstCandidateDate(participants, goals);
        LocalDate lastCandidate = lastCandidateDate(goals, today);

        for (LocalDate date = firstCandidate; !date.isAfter(lastCandidate); date = date.plusDays(1)) {
            if (activeMembers(participants, goals, date) > 0) {
                return date;
            }
        }

        return null;
    }

    public GroupDailyVerification verifyConfirmedDate(
            LocalDate date,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            Set<Long> certifiedParticipantIds
    ) {
        List<GroupActivityParticipant> activeParticipants = activeParticipants(participants, goals, date);
        int activeMemberCount = activeParticipants.size();
        int certifiedMemberCount = (int) activeParticipants.stream()
                .filter(participant -> certifiedParticipantIds.contains(participant.participantId()))
                .count();
        int requiredCount = requiredCount(activeMemberCount);

        return new GroupDailyVerification(
                date,
                CalendarDayStatus.CONFIRMED,
                result(activeMemberCount, certifiedMemberCount, requiredCount),
                activeMemberCount,
                certifiedMemberCount,
                requiredCount
        );
    }

    public GroupActivityCalendarSummary summarizeConfirmedDates(
            LocalDate today,
            LocalDate firstVerificationDate,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            Map<LocalDate, Set<Long>> certifiedParticipantIdsByDate
    ) {
        if (firstVerificationDate == null) {
            return new GroupActivityCalendarSummary(null, null, 0, 0, 0);
        }

        LocalDate endDate = today.minusDays(1);
        if (endDate.isBefore(firstVerificationDate)) {
            return new GroupActivityCalendarSummary(firstVerificationDate, null, 0, 0, 0);
        }

        int allCount = 0;
        int halfCount = 0;
        int resetCount = 0;

        for (LocalDate date = firstVerificationDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            GroupDailyVerification verification = verifyConfirmedDate(
                    date,
                    participants,
                    goals,
                    certifiedParticipantIdsByDate.getOrDefault(date, Set.of())
            );

            if (verification.result() == GroupDailyVerificationResult.ALL) {
                allCount++;
            } else if (verification.result() == GroupDailyVerificationResult.HALF) {
                halfCount++;
            } else {
                resetCount++;
            }
        }

        return new GroupActivityCalendarSummary(firstVerificationDate, endDate, allCount, halfCount, resetCount);
    }

    public int streakDays(
            LocalDate today,
            LocalDate firstVerificationDate,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            Map<LocalDate, Set<Long>> certifiedParticipantIdsByDate
    ) {
        if (firstVerificationDate == null) {
            return 0;
        }

        int streak = 0;
        for (LocalDate date = today.minusDays(1); !date.isBefore(firstVerificationDate); date = date.minusDays(1)) {
            GroupDailyVerification verification = verifyConfirmedDate(
                    date,
                    participants,
                    goals,
                    certifiedParticipantIdsByDate.getOrDefault(date, Set.of())
            );

            if (!verification.result().success()) {
                break;
            }

            streak++;
        }

        return streak;
    }

    public List<MemberDailyGoal> effectiveGoals(Long userId, List<MemberDailyGoal> goals, LocalDate date) {
        Map<UsageGoalTypeCode, MemberDailyGoal> latestByType = new EnumMap<>(UsageGoalTypeCode.class);

        goals.stream()
                .filter(goal -> goal.userId().equals(userId))
                .filter(goal -> !goal.effectiveDate().isAfter(date))
                .forEach(goal -> latestByType.merge(goal.usageGoalType(), goal, this::laterGoal));

        return latestByType.values().stream()
                .sorted(Comparator.comparing(MemberDailyGoal::usageGoalType))
                .toList();
    }

    public boolean includedInGroupResult(
            GroupActivityParticipant participant,
            List<MemberDailyGoal> goals,
            LocalDate date
    ) {
        return participant.presentForWholeDay(date)
                && !effectiveGoals(participant.userId(), goals, date).isEmpty();
    }

    private List<GroupActivityParticipant> activeParticipants(
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            LocalDate date
    ) {
        return participants.stream()
                .filter(participant -> includedInGroupResult(participant, goals, date))
                .toList();
    }

    private int activeMembers(List<GroupActivityParticipant> participants, List<MemberDailyGoal> goals, LocalDate date) {
        return activeParticipants(participants, goals, date).size();
    }

    private GroupDailyVerificationResult result(int activeMemberCount, int certifiedMemberCount, int requiredCount) {
        if (activeMemberCount == 0 || certifiedMemberCount < requiredCount) {
            return GroupDailyVerificationResult.RESET;
        }
        if (certifiedMemberCount == activeMemberCount) {
            return GroupDailyVerificationResult.ALL;
        }
        return GroupDailyVerificationResult.HALF;
    }

    private int requiredCount(int activeMemberCount) {
        return (activeMemberCount + 1) / 2;
    }

    private LocalDate firstCandidateDate(List<GroupActivityParticipant> participants, List<MemberDailyGoal> goals) {
        LocalDate firstParticipantDate = participants.stream()
                .flatMap(participant -> java.util.stream.Stream.of(
                        participant.memberJoinedAt(),
                        participant.participantJoinedAt()
                ))
                .filter(java.util.Objects::nonNull)
                .map(joinedAt -> joinedAt.toLocalDate().plusDays(1))
                .min(LocalDate::compareTo)
                .orElse(LocalDate.MAX);

        LocalDate firstGoalDate = goals.stream()
                .map(MemberDailyGoal::effectiveDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.MAX);

        return firstParticipantDate.isBefore(firstGoalDate) ? firstParticipantDate : firstGoalDate;
    }

    private LocalDate lastCandidateDate(List<MemberDailyGoal> goals, LocalDate today) {
        return goals.stream()
                .map(MemberDailyGoal::effectiveDate)
                .max(LocalDate::compareTo)
                .filter(date -> date.isAfter(today))
                .orElse(today);
    }

    private MemberDailyGoal laterGoal(MemberDailyGoal left, MemberDailyGoal right) {
        return left.setAt().isAfter(right.setAt()) ? left : right;
    }
}
