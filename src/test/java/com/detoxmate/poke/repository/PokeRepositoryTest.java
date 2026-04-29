package com.detoxmate.poke.repository;

import com.detoxmate.poke.domain.Poke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PokeRepositoryTest {

    @Autowired
    private PokeRepository pokeRepository;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;

    private static final Long SENDER_USER_ID = 1L;
    private static final Long RECEIVER_USER_ID = 2L;
    private static final Long OTHER_RECEIVER_USER_ID = 3L;


    private static final LocalDate POKE_DATE = LocalDate.of(2026, 4, 30);

    @Test
    @DisplayName("찌르기를 저장하면 ID가 부여된다")
    void savePoke_assignsId() {
        // given
        Poke poke = Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE);

        // when
        Poke saved = pokeRepository.save(poke);

        // then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("같은 그룹 챌린지에서 같은 보낸 사람, 받은 사람, 날짜의 찌르기가 있으면 true를 반환한다")
    void existsPoke_returnsTrueWhenSameReceiverWasPokedOnDate() {
        // given
        pokeRepository.save(Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE));

        // when
        boolean exists = pokeRepository.existsPoke(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("같은 보낸 사람이라도 받은 사람이 다르면 false를 반환한다")
    void existsPoke_returnsFalseWhenReceiverIsDifferent() {
        // given
        pokeRepository.save(Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE));

        // when
        boolean exists = pokeRepository.existsPoke(GROUP_CHALLENGE_ID, SENDER_USER_ID, OTHER_RECEIVER_USER_ID, POKE_DATE);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("같은 보낸 사람과 받은 사람이라도 그룹 챌린지가 다르면 false를 반환한다")
    void existsPoke_returnsFalseWhenGroupChallengeIsDifferent() {
        // given
        pokeRepository.save(Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE));

        // when
        boolean exists = pokeRepository.existsPoke(OTHER_GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("같은 보낸 사람과 받은 사람이라도 날짜가 다르면 false를 반환한다")
    void existsPoke_returnsFalseWhenDateIsDifferent() {
        // given
        pokeRepository.save(Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE));

        // when
        boolean exists = pokeRepository.existsPoke(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE.plusDays(1));

        // then
        assertThat(exists).isFalse();
    }

}
