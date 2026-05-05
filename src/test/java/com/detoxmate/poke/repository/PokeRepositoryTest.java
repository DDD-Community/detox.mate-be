package com.detoxmate.poke.repository;

import com.detoxmate.poke.domain.Poke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PokeRepositoryTest {

    @Autowired
    PokeRepository pokeRepository;

    private static final Long CHALLENGE_RECORD_ID = 1L;
    private static final Long OTHER_CHALLENGE_RECORD_ID = 2L;

    private static final Long SENDER_USER_ID = 10L;
    private static final Long RECEIVER_USER_ID = 20L;
    private static final Long OTHER_RECEIVER_USER_ID = 30L;

    private static final LocalDate POKE_DATE = LocalDate.of(2026, 5, 1);

    @Test
    @DisplayName("같은 챌린지 기록에서 같은 sender가 같은 receiver를 이미 찔렀는지 확인한다")
    void existsPoke_returnsTrueWhenSameSenderPokedSameReceiverInChallengeRecord() {
        // given
        pokeRepository.save(
                Poke.create(CHALLENGE_RECORD_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE)
        );

        // when
        boolean exists = pokeRepository.existsPoke(CHALLENGE_RECORD_ID, SENDER_USER_ID, RECEIVER_USER_ID);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("같은 sender라도 receiver가 다르면 중복 찌르기가 아니다")
    void existsPoke_returnsFalseWhenReceiverIsDifferent() {
        // given
        pokeRepository.save(
                Poke.create(CHALLENGE_RECORD_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE)
        );

        // when
        boolean exists = pokeRepository.existsPoke(CHALLENGE_RECORD_ID, SENDER_USER_ID, OTHER_RECEIVER_USER_ID);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("같은 sender와 receiver라도 챌린지 기록이 다르면 중복 찌르기가 아니다")
    void existsPoke_returnsFalseWhenChallengeRecordIsDifferent() {
        // given
        pokeRepository.save(
                Poke.create(CHALLENGE_RECORD_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE)
        );

        // when
        boolean exists = pokeRepository.existsPoke(OTHER_CHALLENGE_RECORD_ID, SENDER_USER_ID, RECEIVER_USER_ID);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("챌린지 기록의 콕 찌르기 목록을 최신순으로 조회한다")
    void findAllByChallengeRecordOrderByLatest_returnsPokesOrderByPokeDateDescAndIdDesc() {
        // given
        Poke old = pokeRepository.save(
                Poke.create(CHALLENGE_RECORD_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE.minusDays(1))
        );
        Poke firstToday = pokeRepository.save(
                Poke.create(CHALLENGE_RECORD_ID, 40L, RECEIVER_USER_ID, POKE_DATE)
        );
        Poke secondToday = pokeRepository.save(
                Poke.create(CHALLENGE_RECORD_ID, 50L, RECEIVER_USER_ID, POKE_DATE)
        );
        pokeRepository.save(
                Poke.create(OTHER_CHALLENGE_RECORD_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE.plusDays(1))
        );

        // when
        List<Poke> pokes = pokeRepository.findAllByChallengeRecordOrderByLatest(CHALLENGE_RECORD_ID);

        // then
        assertThat(pokes)
                .extracting(Poke::getId)
                .containsExactly(secondToday.getId(), firstToday.getId(), old.getId());
    }


}
