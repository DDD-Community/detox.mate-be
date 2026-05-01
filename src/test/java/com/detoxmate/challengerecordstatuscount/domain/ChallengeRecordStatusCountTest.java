package com.detoxmate.challengerecordstatuscount.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecordstatuscount.ChallengeRecordStatusCountErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChallengeRecordStatusCountTest {

    private static final Long CHALLENGE_RECORD_ID = 100L;

    @Test
    @DisplayName("챌린지 기록 집계는 모든 count가 0인 상태로 생성된다")
    void create_initializesCountsToZero() {
        // when
        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID);

        // then
        assertThat(statusCount.getChallengeRecordId()).isEqualTo(CHALLENGE_RECORD_ID);
        assertThat(statusCount.getBeforeCommentCount()).isZero();
        assertThat(statusCount.getAfterCommentCount()).isZero();
        assertThat(statusCount.getReactionCount()).isZero();
        assertThat(statusCount.getPokeCount()).isZero();
        assertThat(statusCount.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("챌린지 기록 ID가 없으면 생성할 수 없다")
    void create_failsWhenChallengeRecordIdIsNull() {
        assertThatThrownBy(() -> ChallengeRecordStatusCount.create(null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordStatusCountErrorCode.CHALLENGE_RECORD_REQUIRED);
    }

    @Test
    @DisplayName("인증 전 댓글 수를 1 증가시킨다")
    void increaseBeforeCommentCount() {
        // given
        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID);

        // when
        statusCount.increaseBeforeCommentCount();

        // then
        assertThat(statusCount.getBeforeCommentCount()).isEqualTo(1);
        assertThat(statusCount.getAfterCommentCount()).isZero();
        assertThat(statusCount.getReactionCount()).isZero();
        assertThat(statusCount.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("인증 후 댓글 수를 1 증가시킨다")
    void increaseAfterCommentCount() {
        // given
        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID);

        // when
        statusCount.increaseAfterCommentCount();

        // then
        assertThat(statusCount.getBeforeCommentCount()).isZero();
        assertThat(statusCount.getAfterCommentCount()).isEqualTo(1);
        assertThat(statusCount.getReactionCount()).isZero();
        assertThat(statusCount.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("리액션 수를 1 증가시킨다")
    void increaseReactionCount() {
        // given
        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID);

        // when
        statusCount.increaseReactionCount();

        // then
        assertThat(statusCount.getBeforeCommentCount()).isZero();
        assertThat(statusCount.getAfterCommentCount()).isZero();
        assertThat(statusCount.getReactionCount()).isEqualTo(1);
        assertThat(statusCount.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("찌르기 수를 1 증가시킨다")
    void increasePokeCount() {
        // given
        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID);

        // when
        statusCount.increasePokeCount();

        // then
        assertThat(statusCount.getBeforeCommentCount()).isZero();
        assertThat(statusCount.getAfterCommentCount()).isZero();
        assertThat(statusCount.getReactionCount()).isZero();
        assertThat(statusCount.getPokeCount()).isEqualTo(1);
    }

}
