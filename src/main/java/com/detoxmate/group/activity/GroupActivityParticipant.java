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
        if (joinedAt == null || !joinedAt.toLocalDate().isBefore(date)) {
            return false;
        }

        if (endedAt == null) {
            return activeStatus.equals(currentStatus);
        }

        return endedAt.toLocalDate().isAfter(date);
    }
}
