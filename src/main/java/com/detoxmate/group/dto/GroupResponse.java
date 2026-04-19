package com.detoxmate.group.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GroupResponse(
        Long id,
        String inviteCode,
        String name,
        String myRole,
        List<GroupMemberResponse> members,
        GroupChallengeSummaryResponse currentChallenge,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
