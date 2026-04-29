package com.detoxmate.activityrecordchallengestatus.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ActivityRecordChallengeStatusTest {

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long ACTIVITY_RECORD_ID = 100L;

    @Test
    @DisplayName("상태를 생성하면 댓글, 리액션, 찌르기 수는 0으로 시작한다")
    void create_initializesCountsToZero() {
        //given & when
        ActivityRecordChallengeStatus status = ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //then
        assertThat(status.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(status.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(status.getCommentCount()).isZero();
        assertThat(status.getReactionCount()).isZero();
        assertThat(status.getPokeCount()).isZero();
        assertThat(status.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 수를 1 증가시킨다")
    void increaseCommentCount() {
        //given
        ActivityRecordChallengeStatus status = ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //when
        status.increaseCommentCount();

        //then
        assertThat(status.getCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("리액션 수를 1 증가시킨다")
    void increaseReactionCount() {
        //given
        ActivityRecordChallengeStatus status = ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //when
        status.increaseReactionCount();

        //then
        assertThat(status.getReactionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("찌르기 수를 1 증가시킨다")
    void increasePokeCount() {
        //given
        ActivityRecordChallengeStatus status = ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //when
        status.increasePokeCount();

        //then
        assertThat(status.getPokeCount()).isEqualTo(1);
    }

}
