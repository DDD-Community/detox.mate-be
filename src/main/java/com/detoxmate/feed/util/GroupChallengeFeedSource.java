package com.detoxmate.feed.util;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;

import java.util.List;

public record GroupChallengeFeedSource(
        GroupChallenge challenge,
        Group group,
        List<GroupChallengeParticipantResponse> participants
) {
}
