package com.detoxmate.reaction.repository;

import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.domain.ReactionBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ReactionRepositoryTest {

    @Autowired
    ReactionRepository reactionRepository;

    private static final Long CHALLENGE_RECORD_ID = 1L;
    private static final Long OTHER_CHALLENGE_RECORD_ID = 2L;

    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;

    @Test
    @DisplayName("챌린지 기록의 삭제되지 않은 리액션만 최신순으로 조회한다")
    void findActiveByChallengeRecordOrderByLatest_returnsOnlyActiveReactionsOrderByLatest() {
        // given
        Reaction first = reactionRepository.save(
                Reaction.create(CHALLENGE_RECORD_ID, USER_ID, ReactionBody.CLAP)
        );
        Reaction second = reactionRepository.save(
                Reaction.create(CHALLENGE_RECORD_ID, OTHER_USER_ID, ReactionBody.MUSCLE)
        );

        Reaction deleted = reactionRepository.save(
                Reaction.create(CHALLENGE_RECORD_ID, 30L, ReactionBody.CLAP)
        );
        deleted.deleteBy(30L);

        reactionRepository.save(
                Reaction.create(OTHER_CHALLENGE_RECORD_ID, USER_ID, ReactionBody.CLAP)
        );

        // when
        List<Reaction> reactions = reactionRepository.findActiveByChallengeRecordOrderByLatest(CHALLENGE_RECORD_ID);

        // then
        assertThat(reactions)
                .extracting(Reaction::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("같은 챌린지 기록에 같은 사용자의 같은 종류 리액션이 활성 상태로 존재하는지 확인한다")
    void existsActiveReaction_returnsTrueWhenSameUserAndSameBodyExists() {
        // given
        reactionRepository.save(
                Reaction.create(CHALLENGE_RECORD_ID, USER_ID, ReactionBody.CLAP)
        );

        // when
        boolean exists = reactionRepository.existsActiveReaction(
                CHALLENGE_RECORD_ID,
                USER_ID,
                ReactionBody.CLAP
        );

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("같은 사용자가 같은 챌린지 기록에 다른 종류 리액션을 남기는 것은 중복이 아니다")
    void existsActiveReaction_returnsFalseWhenBodyIsDifferent() {
        // given
        reactionRepository.save(
                Reaction.create(CHALLENGE_RECORD_ID, USER_ID, ReactionBody.CLAP)
        );

        // when
        boolean exists = reactionRepository.existsActiveReaction(
                CHALLENGE_RECORD_ID,
                USER_ID,
                ReactionBody.MUSCLE
        );

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("삭제된 리액션은 중복 검사에서 제외된다")
    void existsActiveReaction_ignoresDeletedReaction() {
        // given
        Reaction reaction = reactionRepository.save(
                Reaction.create(CHALLENGE_RECORD_ID, USER_ID, ReactionBody.CLAP)
        );
        reaction.deleteBy(USER_ID);

        // when
        boolean exists = reactionRepository.existsActiveReaction(
                CHALLENGE_RECORD_ID,
                USER_ID,
                ReactionBody.CLAP
        );

        // then
        assertThat(exists).isFalse();
    }

}
