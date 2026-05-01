package com.detoxmate.challengerecordstatuscount.repository;

import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ChallengeRecordStatusCountRepositoryTest {

    @Autowired
    private ChallengeRecordStatusCountRepository statusCountRepository;

    private static final Long CHALLENGE_RECORD_ID = 100L;
    private static final Long OTHER_CHALLENGE_RECORD_ID = 200L;

    @Test
    @DisplayName("챌린지 기록 집계를 저장하면 ID가 부여된다")
    void saveStatusCount_assignsId() {
        // given
        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID);

        // when
        ChallengeRecordStatusCount saved = statusCountRepository.save(statusCount);

        // then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("챌린지 기록 ID로 집계를 조회한다")
    void findByChallengeRecordId_returnsMatchingStatusCount() {
        // given
        ChallengeRecordStatusCount statusCount = statusCountRepository.save(ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID));

        // when
        Optional<ChallengeRecordStatusCount> found =
                statusCountRepository.findByChallengeRecordId(CHALLENGE_RECORD_ID);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(statusCount.getId());
    }

    @Test
    @DisplayName("다른 챌린지 기록의 집계는 조회되지 않는다")
    void findByChallengeRecordId_doesNotReturnOtherChallengeRecordStatusCount() {
        // given
        statusCountRepository.save(ChallengeRecordStatusCount.create(OTHER_CHALLENGE_RECORD_ID));

        // when
        Optional<ChallengeRecordStatusCount> found = statusCountRepository.findByChallengeRecordId(CHALLENGE_RECORD_ID);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("집계가 없으면 Optional.empty를 반환한다")
    void findByChallengeRecordId_returnsEmptyWhenStatusCountDoesNotExist() {
        // when
        Optional<ChallengeRecordStatusCount> found = statusCountRepository.findByChallengeRecordId(CHALLENGE_RECORD_ID);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("증가된 집계 값이 저장된다")
    void saveStatusCount_persistsCounts() {
        // given
        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(CHALLENGE_RECORD_ID);

        statusCount.increaseBeforeCommentCount();
        statusCount.increaseAfterCommentCount();
        statusCount.increaseReactionCount();
        statusCount.increasePokeCount();

        ChallengeRecordStatusCount saved = statusCountRepository.saveAndFlush(statusCount);

        // when
        ChallengeRecordStatusCount found =
                statusCountRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getBeforeCommentCount()).isEqualTo(1);
        assertThat(found.getAfterCommentCount()).isEqualTo(1);
        assertThat(found.getReactionCount()).isEqualTo(1);
        assertThat(found.getPokeCount()).isEqualTo(1);
    }
}
