package com.detoxmate.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GroupActivityFeedResponse(
        Long groupId,
        LocalDate date,
        GroupDailyVerificationSummaryResponse dailySummary,
        List<MemberResponse> members
) {

    public record MemberResponse(
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
            @JsonInclude(JsonInclude.Include.NON_NULL) Long challengeRecordId,
            ActivityRecordResponse activityRecord,
            int reactionCount,
            int commentCount,
            int pokeCount,
            @JsonProperty("isPoked") boolean isPoked,
            @JsonInclude(JsonInclude.Include.NON_NULL) ReactionSummaryResponse reactions
    ) {
    }

    public record ActivityRecordResponse(
            LocalDateTime submittedAt,
            String activityImageUrl,
            String reflectionText,
            boolean allAchieved,
            List<ActivityRecordDetailHistoryResponse> details
    ) {
    }

    public record ReactionSummaryResponse(
            int totalCount,
            List<ReactionResponse> summary
    ) {
    }

    public record ReactionResponse(
            String reactionBody,
            Long userId,
            String displayName,
            String profileImageUrl
    ) {
    }
}
