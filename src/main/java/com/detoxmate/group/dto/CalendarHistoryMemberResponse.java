package com.detoxmate.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CalendarHistoryMemberResponse(
        Long groupMemberId,
        Long groupChallengeParticipantId,
        Long userId,
        String displayName,
        String profileImageUrl,
        @JsonProperty("isMe") boolean isMe,
        String memberStatus,
        String participantStatus,
        String dailyStatus,
        boolean includedInGroupResult,
        List<MemberDailyGoalResponse> goals,
        ActivityRecordHistoryResponse activityRecord
) {
}
