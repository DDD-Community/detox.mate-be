package com.detoxmate.group.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupChallengeTest {

    @Test
    void 그룹의_첫_챌린지는_1번_챌린지다() {
        Long groupId = 10L;

        GroupChallenge challenge = GroupChallenge.createFirst(groupId);

        assertThat(challenge.getGroupId()).isEqualTo(groupId);
        assertThat(challenge.getChallengeNo()).isEqualTo(1);
    }

    @Test
    void 첫_챌린지는_RECRUITING_상태로_생성된다() {
        Long groupId = 10L;

        GroupChallenge challenge = GroupChallenge.createFirst(groupId);

        assertThat(challenge.getStatus()).isEqualTo(GroupChallengeStatus.RECRUITING.name());
    }
}
