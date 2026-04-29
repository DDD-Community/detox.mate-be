package com.detoxmate.poke.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.poke.PokeErrorCode;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.repository.PokeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PokeServiceTest {

    @Autowired
    private PokeService pokeService;

    @Autowired
    private PokeRepository pokeRepository;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;

    private static final Long SENDER_USER_ID = 1L;
    private static final Long RECEIVER_USER_ID = 2L;
    private static final Long OTHER_RECEIVER_USER_ID = 3L;

    @Test
    @DisplayName("찌르기를 하면 해당 그룹 챌린지의 받은 사람에게 찌르기가 저장된다")
    void poke_persistsPoke() {
        // when
        pokeService.poke(GROUP_CHALLENGE_ID, RECEIVER_USER_ID, SENDER_USER_ID);

        // then
        assertThat(pokeRepository.count()).isEqualTo(1);

        Poke saved = pokeRepository.findAll().get(0);
        assertThat(saved.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(saved.getSenderUserId()).isEqualTo(SENDER_USER_ID);
        assertThat(saved.getReceiverUserId()).isEqualTo(RECEIVER_USER_ID);
        assertThat(saved.getPokeDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 그룹 챌린지에서 같은 사람에게 같은 날짜에 두 번 찌를 수 없다")
    void poke_throwsExceptionWhenSameReceiverAlreadyPokedToday() {
        // given
        pokeService.poke(GROUP_CHALLENGE_ID, RECEIVER_USER_ID, SENDER_USER_ID);

        // when & then
        assertThatThrownBy(() -> pokeService.poke(GROUP_CHALLENGE_ID, RECEIVER_USER_ID, SENDER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_ALREADY_EXISTS);

        assertThat(pokeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 보낸 사람이라도 다른 사람에게는 찌를 수 있다")
    void poke_allowsDifferentReceiver() {
        // given
        pokeService.poke(GROUP_CHALLENGE_ID, RECEIVER_USER_ID, SENDER_USER_ID);

        // when
        pokeService.poke(GROUP_CHALLENGE_ID, OTHER_RECEIVER_USER_ID, SENDER_USER_ID);

        // then
        assertThat(pokeRepository.count()).isEqualTo(2);

        assertThat(pokeRepository.existsPoke(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, LocalDate.now())).isTrue();

        assertThat(pokeRepository.existsPoke(GROUP_CHALLENGE_ID, SENDER_USER_ID, OTHER_RECEIVER_USER_ID, LocalDate.now())).isTrue();
    }

    @Test
    @DisplayName("같은 보낸 사람과 받은 사람이라도 그룹 챌린지가 다르면 찌를 수 있다")
    void poke_allowsSameReceiverInDifferentGroupChallenge() {
        // given
        pokeService.poke(GROUP_CHALLENGE_ID, RECEIVER_USER_ID, SENDER_USER_ID);

        // when
        pokeService.poke(OTHER_GROUP_CHALLENGE_ID, RECEIVER_USER_ID, SENDER_USER_ID);

        // then
        assertThat(pokeRepository.count()).isEqualTo(2);

        assertThat(pokeRepository.existsPoke(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, LocalDate.now())).isTrue();

        assertThat(pokeRepository.existsPoke(OTHER_GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, LocalDate.now())).isTrue();
    }

    @Test
    @DisplayName("자기 자신은 찌를 수 없다")
    void poke_throwsExceptionWhenSenderAndReceiverAreSame() {
        // when & then
        assertThatThrownBy(() -> pokeService.poke(GROUP_CHALLENGE_ID, SENDER_USER_ID, SENDER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_SELF_NOT_ALLOWED);

        assertThat(pokeRepository.count()).isZero();
    }

}
