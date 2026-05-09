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

    // 그룹 인증 평가를 시작할 수 있는 첫 KST 날짜를 산정한다.
    public LocalDate firstVerificationDate(
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            LocalDate today
    ) {
        // 참여자나 목표 이력이 없으면 유효한 활동중 멤버를 만들 수 없으므로 시작일을 산정하지 않는다.
        if (participants.isEmpty() || goals.isEmpty()) {
            return null;
        }

        // 합류 당일과 목표 설정 당일은 제외되므로, 둘 다 다음날부터 후보가 된다.
        LocalDate firstCandidate = firstCandidateDate(participants, goals);
        // 목표 적용일이 미래라면, 아직 오지 않은 시작 예정일도 산정할 수 있게 탐색 범위에 포함한다.
        LocalDate lastCandidate = lastCandidateDate(goals, today);

        for (LocalDate date = firstCandidate; !date.isAfter(lastCandidate); date = date.plusDays(1)) {
            // 그룹 인증 평가는 활동중 멤버가 1명 이상 존재할 수 있는 첫 날짜부터 시작한다.
            if (activeMembers(participants, goals, date) > 0) {
                return date;
            }
        }

        return null;
    }

    // 확정된 날짜의 활동중 멤버 수, 인증자 수, 필요 인증 수, ALL/HALF/RESET 결과를 계산한다.
    public GroupDailyVerification verifyConfirmedDate(
            LocalDate date,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            Set<Long> certifiedParticipantIds
    ) {
        // 인증자 수는 활동중 멤버만 대상으로 센다.
        List<GroupActivityParticipant> activeParticipants = activeParticipants(participants, goals, date);
        int activeMemberCount = activeParticipants.size();
        int certifiedMemberCount = (int) activeParticipants.stream()
                // 활동중 멤버가 아닌 유저의 인증 기록은 그룹 인증 계산에 포함하지 않는다.
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

    // 첫 인증 시작일 이후 어제까지의 확정 날짜를 ALL/HALF/RESET 누적 카운트로 집계한다.
    public GroupActivityCalendarSummary summarizeConfirmedDates(
            LocalDate today,
            LocalDate firstVerificationDate,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            Map<LocalDate, Set<Long>> certifiedParticipantIdsByDate
    ) {
        // 첫 인증 시작일을 산정할 수 없으면 누적 카운트도 계산하지 않는다.
        if (firstVerificationDate == null) {
            return new GroupActivityCalendarSummary(null, null, 0, 0, 0);
        }

        // 오늘은 아직 확정되지 않았으므로 상단 누적 카운트의 종료일은 어제다.
        LocalDate endDate = today.minusDays(1);
        // 첫 인증 시작일이 오늘 이후라면 아직 확정된 날짜가 없다.
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

            // 상단 카운트는 첫 인증 시작일 이후의 일별 결과를 ALL/HALF/RESET으로 배타적으로 센다.
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

    // 오늘을 제외하고 어제부터 과거로 연속 성공(ALL 또는 HALF)한 그룹 스트릭 일수를 계산한다.
    public int streakDays(
            LocalDate today,
            LocalDate firstVerificationDate,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals,
            Map<LocalDate, Set<Long>> certifiedParticipantIdsByDate
    ) {
        // 평가 시작일이 없으면 연속 성공을 셀 기준도 없다.
        if (firstVerificationDate == null) {
            return 0;
        }

        int streak = 0;
        // 오늘 인증 상태는 스트릭에 반영하지 않으므로 어제부터 과거로 확인한다.
        for (LocalDate date = today.minusDays(1); !date.isBefore(firstVerificationDate); date = date.minusDays(1)) {
            GroupDailyVerification verification = verifyConfirmedDate(
                    date,
                    participants,
                    goals,
                    certifiedParticipantIdsByDate.getOrDefault(date, Set.of())
            );

            // 그룹 인증 성공은 ALL 또는 HALF이며, RESET을 만나면 스트릭은 끊긴다.
            if (!verification.result().success()) {
                break;
            }

            streak++;
        }

        return streak;
    }

    // 목표 설정/수정 다음날부터 적용되는 정책에 따라, 조회 날짜에 유효한 타입별 최신 목표를 고른다.
    public List<MemberDailyGoal> effectiveGoals(Long userId, List<MemberDailyGoal> goals, LocalDate date) {
        Map<UsageGoalTypeCode, MemberDailyGoal> latestByType = new EnumMap<>(UsageGoalTypeCode.class);

        goals.stream()
                .filter(goal -> goal.userId().equals(userId))
                // 목표는 설정/수정 다음날부터 유효하므로, 조회 날짜에 이미 적용된 목표만 사용한다.
                .filter(goal -> !goal.effectiveDate().isAfter(date))
                // 같은 목표 타입의 이력이 여러 개면 조회 날짜에 유효한 가장 최신 목표를 사용한다.
                .forEach(goal -> latestByType.merge(goal.usageGoalType(), goal, this::laterGoal));

        return latestByType.values().stream()
                .sorted(Comparator.comparing(MemberDailyGoal::usageGoalType))
                .toList();
    }

    // 해당 날짜 하루 전체 동안 그룹/챌린지에 남아 있고 유효한 목표가 있는 멤버만 그룹 결과 분모에 포함한다.
    public boolean includedInGroupResult(
            GroupActivityParticipant participant,
            List<MemberDailyGoal> goals,
            LocalDate date
    ) {
        // 하루 전체 동안 그룹과 챌린지에 남아 있어야 분모에 포함된다.
        return participant.presentForWholeDay(date)
                // 해당 날짜에 유효한 개인 목표가 있어야 활동중 멤버로 본다.
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
        // 활동중 멤버가 0명이거나 절반 미만이 인증하면 리셋이다.
        if (activeMemberCount == 0 || certifiedMemberCount < requiredCount) {
            return GroupDailyVerificationResult.RESET;
        }
        // 활동중 멤버 전원이 인증하면 전원 인증이다.
        if (certifiedMemberCount == activeMemberCount) {
            return GroupDailyVerificationResult.ALL;
        }
        // 절반 이상이지만 전원은 아니면 절반 인증이다.
        return GroupDailyVerificationResult.HALF;
    }

    private int requiredCount(int activeMemberCount) {
        // 그룹 인증 성공에 필요한 인증자 수는 활동중 멤버 수의 절반 올림이다.
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
