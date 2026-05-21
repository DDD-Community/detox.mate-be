package com.detoxmate.poke.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.poke.PokeErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokeTest {

    private static final Long CHALLENGE_RECORD_ID = 1L;
    private static final Long SENDER_USER_ID = 10L;
    private static final Long RECEIVER_USER_ID = 20L;
    private static final LocalDate POKE_DATE = LocalDate.of(2026, 5, 1);

    @Test
    @DisplayName("콕 찌르기를 생성하면 챌린지 기록, 보낸 사람, 받은 사람, 날짜가 저장된다")
    void create_initializesPoke() {
        // when
        Poke poke = Poke.create(
                CHALLENGE_RECORD_ID,
                SENDER_USER_ID,
                RECEIVER_USER_ID,
                POKE_DATE
        );

        // then
        assertThat(poke.getChallengeRecordId()).isEqualTo(CHALLENGE_RECORD_ID);
        assertThat(poke.getSenderUserId()).isEqualTo(SENDER_USER_ID);
        assertThat(poke.getReceiverUserId()).isEqualTo(RECEIVER_USER_ID);
        assertThat(poke.getPokeDate()).isEqualTo(POKE_DATE);
        assertThat(poke.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("챌린지 기록 ID 없이 콕 찌르기를 생성할 수 없다")
    void create_throwsExceptionWhenChallengeRecordIdIsNull() {
        // when & then
        assertThatThrownBy(() -> Poke.create(null, SENDER_USER_ID, RECEIVER_USER_ID, POKE_DATE))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_CHALLENGE_RECORD_REQUIRED);
    }

    @Test
    @DisplayName("보낸 사람 없이 콕 찌르기를 생성할 수 없다")
    void create_throwsExceptionWhenSenderUserIdIsNull() {
        // when & then
        assertThatThrownBy(() -> Poke.create(CHALLENGE_RECORD_ID, null, RECEIVER_USER_ID, POKE_DATE))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_SENDER_REQUIRED);
    }

    @Test
    @DisplayName("받은 사람 없이 콕 찌르기를 생성할 수 없다")
    void create_throwsExceptionWhenReceiverUserIdIsNull() {
        // when & then
        assertThatThrownBy(() -> Poke.create(CHALLENGE_RECORD_ID, SENDER_USER_ID, null, POKE_DATE))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_RECEIVER_REQUIRED);
    }

    @Test
    @DisplayName("날짜 없이 콕 찌르기를 생성할 수 없다")
    void create_throwsExceptionWhenPokeDateIsNull() {
        // when & then
        assertThatThrownBy(() -> Poke.create(CHALLENGE_RECORD_ID, SENDER_USER_ID, RECEIVER_USER_ID, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_DATE_REQUIRED);
    }

    @Test
    @DisplayName("자기 자신을 콕 찌를 수 없다")
    void create_throwsExceptionWhenSenderAndReceiverAreSame() {
        // when & then
        assertThatThrownBy(() -> Poke.create(CHALLENGE_RECORD_ID, SENDER_USER_ID, SENDER_USER_ID, POKE_DATE))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_SELF_NOT_ALLOWED);
    }

}
