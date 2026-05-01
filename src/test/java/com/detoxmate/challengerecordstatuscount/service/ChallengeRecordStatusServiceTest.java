package com.detoxmate.challengerecordstatuscount.service;

import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecordstatuscount.ChallengeRecordStatusCountErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChallengeRecordStatusServiceTest {

    @Autowired
    ChallengeRecordStatusCountService statusCountService;

    @Autowired
    ChallengeRecordStatusCountRepository statusCountRepository;

    private static final Long CHALLENGE_RECORD_ID = 100L;

    @Test
    @DisplayName("챌린지 기록 집계를 생성하면 모든 count가 0인 상태로 저장된다")
    void create_persistsStatusCountWithZeroCounts() {
        // when
        ChallengeRecordStatusCount created = statusCountService.create(CHALLENGE_RECORD_ID);

        // then
        ChallengeRecordStatusCount found = statusCountRepository.findById(created.getId()).orElseThrow();

        assertThat(found.getChallengeRecordId()).isEqualTo(CHALLENGE_RECORD_ID);
        assertThat(found.getBeforeCommentCount()).isZero();
        assertThat(found.getAfterCommentCount()).isZero();
        assertThat(found.getReactionCount()).isZero();
        assertThat(found.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("인증 전 댓글 수를 1 증가시킨다")
    void increaseBeforeCommentCount_increasesBeforeCommentCount() {
        // given
        ChallengeRecordStatusCount statusCount = saveStatusCount();

        // when
        statusCountService.increaseBeforeCommentCount(CHALLENGE_RECORD_ID);

        // then
        ChallengeRecordStatusCount found = statusCountRepository.findById(statusCount.getId()).orElseThrow();

        assertThat(found.getBeforeCommentCount()).isEqualTo(1);
        assertThat(found.getAfterCommentCount()).isZero();
        assertThat(found.getReactionCount()).isZero();
        assertThat(found.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("인증 후 댓글 수를 1 증가시킨다")
    void increaseAfterCommentCount_increasesAfterCommentCount() {
        // given
        ChallengeRecordStatusCount statusCount = saveStatusCount();

        // when
        statusCountService.increaseAfterCommentCount(CHALLENGE_RECORD_ID);

        // then
        ChallengeRecordStatusCount found = statusCountRepository.findById(statusCount.getId()).orElseThrow();

        assertThat(found.getBeforeCommentCount()).isZero();
        assertThat(found.getAfterCommentCount()).isEqualTo(1);
        assertThat(found.getReactionCount()).isZero();
        assertThat(found.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("리액션 수를 1 증가시킨다")
    void increaseReactionCount_increasesReactionCount() {
        // given
        ChallengeRecordStatusCount statusCount = saveStatusCount();

        // when
        statusCountService.increaseReactionCount(CHALLENGE_RECORD_ID);

        // then
        ChallengeRecordStatusCount found = statusCountRepository.findById(statusCount.getId()).orElseThrow();

        assertThat(found.getBeforeCommentCount()).isZero();
        assertThat(found.getAfterCommentCount()).isZero();
        assertThat(found.getReactionCount()).isEqualTo(1);
        assertThat(found.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("찌르기 수를 1 증가시킨다")
    void increasePokeCount_increasesPokeCount() {
        // given
        ChallengeRecordStatusCount statusCount = saveStatusCount();

        // when
        statusCountService.increasePokeCount(CHALLENGE_RECORD_ID);

        // then
        ChallengeRecordStatusCount found = statusCountRepository.findById(statusCount.getId()).orElseThrow();

        assertThat(found.getBeforeCommentCount()).isZero();
        assertThat(found.getAfterCommentCount()).isZero();
        assertThat(found.getReactionCount()).isZero();
        assertThat(found.getPokeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("챌린지 기록 집계가 없으면 인증 전 댓글 수를 증가시킬 수 없다")
    void increaseBeforeCommentCount_throwsExceptionWhenStatusCountDoesNotExist() {
        assertThatThrownBy(() -> statusCountService.increaseBeforeCommentCount(CHALLENGE_RECORD_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordStatusCountErrorCode.CHALLENGE_RECORD_STATUS_COUNT_NOT_FOUND);
    }

    private ChallengeRecordStatusCount saveStatusCount() {
        return statusCountRepository.save(ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID));
    }

}
