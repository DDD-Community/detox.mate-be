package com.detoxmate.group.activity;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GroupActivityVerificationPolicyTest {

    private final GroupActivityVerificationPolicy policy = new GroupActivityVerificationPolicy();

    @Test
    @DisplayName("활동중 멤버 전원이 인증하면 ALL이다")
    void verifyConfirmedDate_returnsAllWhenEveryActiveMemberCertified() {
        LocalDate date = LocalDate.of(2026, 4, 13);
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null),
                participant(2L, 2L, LocalDate.of(2026, 4, 10), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 11)),
                goal(2L, LocalDate.of(2026, 4, 11))
        );

        GroupDailyVerification result = policy.verifyConfirmedDate(date, participants, goals, Set.of(1L, 2L));

        assertThat(result.result()).isEqualTo(GroupDailyVerificationResult.ALL);
        assertThat(result.activeMemberCount()).isEqualTo(2);
        assertThat(result.certifiedMemberCount()).isEqualTo(2);
        assertThat(result.requiredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("활동중 멤버 중 절반 이상이지만 전원은 아니면 HALF다")
    void verifyConfirmedDate_returnsHalfWhenAtLeastHalfCertified() {
        LocalDate date = LocalDate.of(2026, 4, 13);
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null),
                participant(2L, 2L, LocalDate.of(2026, 4, 10), null),
                participant(3L, 3L, LocalDate.of(2026, 4, 10), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 11)),
                goal(2L, LocalDate.of(2026, 4, 11)),
                goal(3L, LocalDate.of(2026, 4, 11))
        );

        GroupDailyVerification result = policy.verifyConfirmedDate(date, participants, goals, Set.of(1L, 2L));

        assertThat(result.result()).isEqualTo(GroupDailyVerificationResult.HALF);
        assertThat(result.activeMemberCount()).isEqualTo(3);
        assertThat(result.certifiedMemberCount()).isEqualTo(2);
        assertThat(result.requiredCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("활동중 멤버 중 절반 미만이 인증하면 RESET이다")
    void verifyConfirmedDate_returnsResetWhenLessThanHalfCertified() {
        LocalDate date = LocalDate.of(2026, 4, 13);
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null),
                participant(2L, 2L, LocalDate.of(2026, 4, 10), null),
                participant(3L, 3L, LocalDate.of(2026, 4, 10), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 11)),
                goal(2L, LocalDate.of(2026, 4, 11)),
                goal(3L, LocalDate.of(2026, 4, 11))
        );

        GroupDailyVerification result = policy.verifyConfirmedDate(date, participants, goals, Set.of(1L));

        assertThat(result.result()).isEqualTo(GroupDailyVerificationResult.RESET);
        assertThat(result.activeMemberCount()).isEqualTo(3);
        assertThat(result.certifiedMemberCount()).isEqualTo(1);
        assertThat(result.requiredCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("참여 후 다음날과 목표 설정 다음날 중 늦은 날짜부터 첫 인증 시작일을 산정한다")
    void firstVerificationDate_startsAfterGoalSetDate() {
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null),
                participant(2L, 2L, LocalDate.of(2026, 4, 10), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 12)),
                goal(2L, LocalDate.of(2026, 4, 12))
        );

        LocalDate firstVerificationDate = policy.firstVerificationDate(
                participants,
                goals,
                LocalDate.of(2026, 4, 9),
                LocalDate.of(2026, 4, 20)
        );

        assertThat(firstVerificationDate).isEqualTo(LocalDate.of(2026, 4, 13));
    }

    @Test
    @DisplayName("목표가 이미 유효해도 참여 다음날부터 첫 인증 시작일을 산정한다")
    void firstVerificationDate_startsAfterParticipantJoinedDateWhenGoalAlreadyEffective() {
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 13), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 10))
        );

        LocalDate firstVerificationDate = policy.firstVerificationDate(
                participants,
                goals,
                LocalDate.of(2026, 4, 9),
                LocalDate.of(2026, 4, 20)
        );

        assertThat(firstVerificationDate).isEqualTo(LocalDate.of(2026, 4, 14));
    }

    @Test
    @DisplayName("참여와 목표가 이미 유효해도 챌린지 시작 다음날부터 첫 인증 시작일을 산정한다")
    void firstVerificationDate_startsAfterChallengeStartDateWhenParticipantAndGoalAlreadyEffective() {
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 10))
        );

        LocalDate firstVerificationDate = policy.firstVerificationDate(
                participants,
                goals,
                LocalDate.of(2026, 4, 13),
                LocalDate.of(2026, 4, 20)
        );

        assertThat(firstVerificationDate).isEqualTo(LocalDate.of(2026, 4, 14));
    }

    @Test
    @DisplayName("챌린지 시작일이 없으면 첫 인증 시작일을 산정하지 않는다")
    void firstVerificationDate_returnsNullWhenChallengeStartDateIsNull() {
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 10))
        );

        LocalDate firstVerificationDate = policy.firstVerificationDate(
                participants,
                goals,
                null,
                LocalDate.of(2026, 4, 20)
        );

        assertThat(firstVerificationDate).isNull();
    }

    @Test
    @DisplayName("활동중 멤버가 1명이어도 유효한 목표가 있으면 첫 인증 시작일을 산정한다")
    void firstVerificationDate_startsWithSingleActiveMember() {
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 12))
        );

        LocalDate firstVerificationDate = policy.firstVerificationDate(
                participants,
                goals,
                LocalDate.of(2026, 4, 9),
                LocalDate.of(2026, 4, 20)
        );

        assertThat(firstVerificationDate).isEqualTo(LocalDate.of(2026, 4, 13));
    }

    @Test
    @DisplayName("탈퇴한 멤버는 탈퇴 당일부터 활동중 멤버에서 제외된다")
    void verifyConfirmedDate_excludesMemberFromLeftDate() {
        LocalDate date = LocalDate.of(2026, 4, 13);
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null),
                participant(2L, 2L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 13))
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 11)),
                goal(2L, LocalDate.of(2026, 4, 11))
        );

        GroupDailyVerification result = policy.verifyConfirmedDate(date, participants, goals, Set.of(1L));

        assertThat(result.result()).isEqualTo(GroupDailyVerificationResult.ALL);
        assertThat(result.activeMemberCount()).isEqualTo(1);
        assertThat(result.certifiedMemberCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("첫 인증 시작일 이후 확정 날짜의 결과를 누적 카운트하고 어제부터 스트릭을 계산한다")
    void summarizeConfirmedDates_countsSinceFirstVerificationDateAndCalculatesStreak() {
        List<GroupActivityParticipant> participants = List.of(
                participant(1L, 1L, LocalDate.of(2026, 4, 10), null),
                participant(2L, 2L, LocalDate.of(2026, 4, 10), null)
        );
        List<MemberDailyGoal> goals = List.of(
                goal(1L, LocalDate.of(2026, 4, 11)),
                goal(2L, LocalDate.of(2026, 4, 11))
        );
        LocalDate today = LocalDate.of(2026, 4, 16);
        LocalDate firstVerificationDate = LocalDate.of(2026, 4, 12);
        Map<LocalDate, Set<Long>> certifiedByDate = Map.of(
                LocalDate.of(2026, 4, 12), Set.of(1L, 2L),
                LocalDate.of(2026, 4, 13), Set.of(1L),
                LocalDate.of(2026, 4, 14), Set.of(),
                LocalDate.of(2026, 4, 15), Set.of(1L, 2L)
        );

        GroupActivityCalendarSummary summary = policy.summarizeConfirmedDates(
                today,
                firstVerificationDate,
                participants,
                goals,
                certifiedByDate
        );
        int streakDays = policy.streakDays(today, firstVerificationDate, participants, goals, certifiedByDate);

        assertThat(summary).isEqualTo(new GroupActivityCalendarSummary(
                firstVerificationDate,
                LocalDate.of(2026, 4, 15),
                2,
                1,
                1
        ));
        assertThat(streakDays).isEqualTo(1);
    }

    private GroupActivityParticipant participant(
            Long participantId,
            Long userId,
            LocalDate joinedDate,
            LocalDate leftDate
    ) {
        return new GroupActivityParticipant(
                participantId,
                participantId + 100,
                userId,
                leftDate == null ? "ACTIVE" : "LEFT",
                joinedDate.atTime(12, 0),
                leftDate == null ? null : leftDate.atTime(15, 0),
                leftDate == null ? "JOINED" : "WITHDRAWN",
                joinedDate.atTime(12, 0),
                leftDate == null ? null : leftDate.atTime(15, 0)
        );
    }

    private MemberDailyGoal goal(Long userId, LocalDate setDate) {
        return new MemberDailyGoal(
                userId * 10,
                userId,
                UsageGoalTypeCode.TOTAL_USAGE,
                90,
                setDate.plusDays(1),
                setDate.atTime(10, 0)
        );
    }
}
