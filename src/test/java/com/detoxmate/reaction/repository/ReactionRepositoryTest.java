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
    private ReactionRepository reactionRepository;

    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_ACTIVITY_RECORD_ID = 200L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;

    @Test
    @DisplayName("리액션을 저장하면 ID가 부여된다")
    void saveReaction_assignsId() {
        //given
        Reaction reaction = Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 1L, ReactionBody.CLAP);

        //when
        Reaction saved = reactionRepository.save(reaction);

        //then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("같은 activity record라도 다른 group challenge의 리액션은 분리되어 조회된다")
    void findActiveByChallengeRecord_isolatesByGroupChallenge() {
        //given
        Reaction first = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 1L, ReactionBody.CLAP)
        );
        reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, OTHER_GROUP_CHALLENGE_ID, 2L, ReactionBody.CLAP)
        );
        Reaction second = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 3L, ReactionBody.CLAP)
        );

        //when
        List<Reaction> result = reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //then
        assertThat(result)
                .extracting(Reaction::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("같은 group challenge라도 다른 activity record의 리액션은 분리되어 조회된다")
    void findActiveByChallengeRecord_isolatesByActivityRecord() {
        //given
        Reaction target = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 1L, ReactionBody.CLAP)
        );
        reactionRepository.save(
                Reaction.create(OTHER_ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 2L, ReactionBody.CLAP)
        );

        //when
        List<Reaction> result = reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //then
        assertThat(result)
                .extracting(Reaction::getId)
                .containsExactly(target.getId());
    }

    @Test
    @DisplayName("삭제된 리액션은 조회되지 않는다")
    void findActiveByChallengeRecord_excludesDeletedReaction() {
        //given
        Reaction active = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 1L, ReactionBody.CLAP)
        );
        Reaction deleted = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 2L, ReactionBody.CLAP)
        );
        deleted.deleteBy(2L);
        reactionRepository.saveAndFlush(deleted);

        //when
        List<Reaction> result = reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //then
        assertThat(result)
                .extracting(Reaction::getId)
                .containsExactly(active.getId());
    }

    @Test
    @DisplayName("ChallengeRecord의 삭제되지 않은 리액션 개수를 반환한다")
    void countActiveByChallengeRecord_countsOnlyActiveReactions() {
        //given
        reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 1L, ReactionBody.CLAP)
        );

        Reaction deleted = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 2L, ReactionBody.CLAP)
        );
        deleted.deleteBy(2L);
        reactionRepository.saveAndFlush(deleted);

        reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, OTHER_GROUP_CHALLENGE_ID, 3L, ReactionBody.CLAP)
        );
        reactionRepository.save(
                Reaction.create(OTHER_ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 4L, ReactionBody.CLAP)
        );

        //when
        long count = reactionRepository.countActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("리액션이 없는 ChallengeRecord의 리액션 개수는 0이다")
    void countActiveByChallengeRecord_returnsZeroWhenNoReaction() {
        //given & when
        long count = reactionRepository.countActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        //then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("같은 사용자가 같은 ChallengeRecord에 같은 body의 활성 리액션을 가지고 있으면 true를 반환한다")
    void existsActiveReaction_returnsTrueWhenSameUserHasSameActiveBody() {
        // given
        reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 1L, ReactionBody.CLAP)
        );

        // when
        boolean exists = reactionRepository.existsActiveReaction(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, 1L, ReactionBody.CLAP);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("같은 사용자가 같은 ChallengeRecord에 다른 body만 가지고 있으면 false를 반환한다")
    void existsActiveReaction_returnsFalseWhenBodyIsDifferent() {
        // given
        reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 1L, ReactionBody.CLAP)
        );

        // when
        boolean exists = reactionRepository.existsActiveReaction(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, 1L, ReactionBody.HAMMER);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("같은 body라도 삭제된 리액션이면 false를 반환한다")
    void existsActiveReaction_returnsFalseWhenReactionIsDeleted() {
        // given
        Reaction reaction = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, 1L, ReactionBody.CLAP)
        );
        reaction.deleteBy(1L);
        reactionRepository.saveAndFlush(reaction);

        // when
        boolean exists = reactionRepository.existsActiveReaction(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, 1L, ReactionBody.CLAP);

        // then
        assertThat(exists).isFalse();
    }


}
