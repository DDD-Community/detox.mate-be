package com.detoxmate.poke.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.poke.PokeErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class PokeTest {

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long SENDER_USER_ID = 1L;
    private static final Long RECEIVER_USER_ID = 2L;
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 29);

    @Test
    @DisplayName("찌르기를 생성하면 그룹 챌린지, 보낸 사람, 받은 사람, 찌른 날짜가 저장된다")
    void createPoke_savesSenderReceiverAndDate() {
        // when
        Poke poke = Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, TODAY);

        // then
        assertThat(poke.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(poke.getSenderUserId()).isEqualTo(SENDER_USER_ID);
        assertThat(poke.getReceiverUserId()).isEqualTo(RECEIVER_USER_ID);
        assertThat(poke.getPokeDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("찌르기를 생성하면 생성 시간이 설정된다")
    void createPoke_setsCreatedAt() {
        // when
        Poke poke = Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, TODAY);

        // then
        assertThat(poke.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("받은 사람이 없으면 찌르기를 생성할 수 없다")
    void createPoke_failsWhenReceiverIsNull() {
        assertThatThrownBy(() -> Poke.create(
                GROUP_CHALLENGE_ID,
                SENDER_USER_ID,
                null,
                TODAY
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_RECEIVER_REQUIRED);
    }

    @Test
    @DisplayName("보낸 사람이 없으면 찌르기를 생성할 수 없다")
    void createPoke_failsWhenSenderIsNull() {
        assertThatThrownBy(() -> Poke.create(GROUP_CHALLENGE_ID, null, RECEIVER_USER_ID, TODAY))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_SENDER_REQUIRED);
    }

    @Test
    @DisplayName("그룹 챌린지가 없으면 찌르기를 생성할 수 없다")
    void createPoke_failsWhenGroupChallengeIsNull() {
        assertThatThrownBy(() -> Poke.create(null, SENDER_USER_ID, RECEIVER_USER_ID, TODAY))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_GROUP_CHALLENGE_REQUIRED);
    }

    @Test
    @DisplayName("찌른 날짜가 없으면 찌르기를 생성할 수 없다")
    void createPoke_failsWhenPokeDateIsNull() {
        assertThatThrownBy(() -> Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, RECEIVER_USER_ID, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_DATE_REQUIRED);
    }

    @Test
    @DisplayName("자기 자신은 찌를 수 없다")
    void createPoke_failsWhenSenderAndReceiverAreSame() {
        assertThatThrownBy(() -> Poke.create(GROUP_CHALLENGE_ID, SENDER_USER_ID, SENDER_USER_ID, TODAY))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_SELF_NOT_ALLOWED);
    }

}
