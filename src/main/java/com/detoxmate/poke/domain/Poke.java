package com.detoxmate.poke.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.poke.PokeErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "pokes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poke {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poke_id")
    private Long id;

    @Column(name = "challenge_record_id", nullable = false)
    private Long challengeRecordId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(name = "poke_date", nullable = false)
    private LocalDate pokeDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Poke(Long challengeRecordId, Long senderUserId, Long receiverUserId, LocalDate pokeDate) {
        validateCreate(challengeRecordId, senderUserId, receiverUserId, pokeDate);

        this.challengeRecordId = challengeRecordId;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.pokeDate = pokeDate;
        this.createdAt = LocalDateTime.now();
    }

    public static Poke create(Long challengeRecordId,
                              Long senderUserId,
                              Long receiverUserId,
                              LocalDate pokeDate) {

        return new Poke(challengeRecordId, senderUserId, receiverUserId, pokeDate);
    }

    private static void validateCreate(Long challengeRecordId,
                                       Long senderUserId,
                                       Long receiverUserId,
                                       LocalDate pokeDate) {

        if (challengeRecordId == null) {
            throw new CustomException(PokeErrorCode.POKE_CHALLENGE_RECORD_REQUIRED);
        }

        if (senderUserId == null) {
            throw new CustomException(PokeErrorCode.POKE_SENDER_REQUIRED);
        }

        if (receiverUserId == null) {
            throw new CustomException(PokeErrorCode.POKE_RECEIVER_REQUIRED);
        }

        if (pokeDate == null) {
            throw new CustomException(PokeErrorCode.POKE_DATE_REQUIRED);
        }

        if (senderUserId.equals(receiverUserId)) {
            throw new CustomException(PokeErrorCode.POKE_SELF_NOT_ALLOWED);
        }
    }
}
