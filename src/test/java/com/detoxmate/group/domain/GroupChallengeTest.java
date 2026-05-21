package com.detoxmate.group.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

        assertThat(challenge.getStatus()).isEqualTo(GroupChallengeStatus.RECRUITING);
    }

    @Test
    void 챌린지를_취소하면_CANCELED_상태와_endAt이_설정된다() {
        GroupChallenge challenge = GroupChallenge.createFirst(10L);

        challenge.cancel();

        assertThat(challenge.getStatus()).isEqualTo(GroupChallengeStatus.CANCELED);
        assertThat(challenge.getEndAt()).isNotNull();
    }

    @Test
    void 챌린지를_활성화하면_ACTIVE_상태와_startAt이_설정된다() {
        GroupChallenge challenge = GroupChallenge.createFirst(10L);
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 1, 0, 0);

        challenge.activate(startAt);

        assertThat(challenge.getStatus()).isEqualTo(GroupChallengeStatus.ACTIVE);
        assertThat(challenge.getStartAt()).isEqualTo(startAt);
    }

    @Test
    void 상태는_MySQL_enum이_아니라_varchar로_저장한다() throws NoSuchFieldException {
        Field statusField = GroupChallenge.class.getDeclaredField("status");

        JdbcTypeCode jdbcTypeCode = statusField.getAnnotation(JdbcTypeCode.class);

        assertThat(jdbcTypeCode).isNotNull();
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.VARCHAR);
    }
}
