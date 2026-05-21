package com.detoxmate.group.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupChallengeParticipantTest {

    @Test
    void 챌린지_참여_시_JOINED_상태로_생성된다() {
        Long groupMemberId = 1L;
        Long groupChallengeId = 10L;

        GroupChallengeParticipant participant = GroupChallengeParticipant.join(groupMemberId, groupChallengeId);

        assertThat(participant.getGroupMemberId()).isEqualTo(groupMemberId);
        assertThat(participant.getGroupChallengeId()).isEqualTo(groupChallengeId);
        assertThat(participant.getStatus()).isEqualTo(GroupChallengeParticipantStatus.JOINED.name());
    }

    @Test
    void 챌린지_참여_시_joinedAt이_설정된다() {
        Long groupMemberId = 1L;
        Long groupChallengeId = 10L;

        GroupChallengeParticipant participant = GroupChallengeParticipant.join(groupMemberId, groupChallengeId);

        assertThat(participant.getJoinedAt()).isNotNull();
    }
}
