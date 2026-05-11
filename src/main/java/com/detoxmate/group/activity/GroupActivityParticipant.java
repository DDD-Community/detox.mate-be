package com.detoxmate.group.activity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GroupActivityParticipant(
        Long participantId,
        Long groupMemberId,
        Long userId,
        String memberStatus,
        LocalDateTime memberJoinedAt,
        LocalDateTime memberLeftAt,
        String participantStatus,
        LocalDateTime participantJoinedAt,
        LocalDateTime participantWithdrawnAt
) {

    private static final String ACTIVE_MEMBER_STATUS = "ACTIVE";
    private static final String JOINED_PARTICIPANT_STATUS = "JOINED";

    public boolean presentForWholeDay(LocalDate date) {
        return activeForWholeDay(memberJoinedAt, memberLeftAt, memberStatus, ACTIVE_MEMBER_STATUS, date)
                && activeForWholeDay(participantJoinedAt, participantWithdrawnAt, participantStatus, JOINED_PARTICIPANT_STATUS, date);
    }

    private boolean activeForWholeDay(
            LocalDateTime joinedAt,
            LocalDateTime endedAt,
            String currentStatus,
            String activeStatus,
            LocalDate date
    ) {
        // 당일 합류자는 그날 하루 전체를 함께한 것이 아니므로 다음 KST 날짜부터 분모 후보가 된다.
        if (joinedAt == null || !joinedAt.toLocalDate().isBefore(date)) {
            return false;
        }

        // 아직 종료 이력이 없으면 현재 상태가 ACTIVE/JOINED일 때만 계속 활동중으로 본다.
        if (endedAt == null) {
            return activeStatus.equals(currentStatus);
        }

        // 당일 탈퇴/이탈자는 그날부터 분모에서 제외한다.
        return endedAt.toLocalDate().isAfter(date);
    }
}
