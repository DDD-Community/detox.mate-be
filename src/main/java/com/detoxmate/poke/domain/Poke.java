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

    @Column(name = "group_challenge_id", nullable = false)
    private Long groupChallengeId;

    @Column(name = "activity_record_id", nullable = false)
    private Long activityRecordId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "poke_date", nullable = false)
    private LocalDate pokeDate;

    private Poke(Long groupChallengeId, Long activityRecordId, Long senderUserId, Long receiverUserId, LocalDate pokeDate) {
        this.groupChallengeId = groupChallengeId;
        this.activityRecordId = activityRecordId;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.pokeDate = pokeDate;
        this.createdAt = LocalDateTime.now();
    }

    public static Poke create(Long groupChallengeId, Long activityRecordId, Long senderUserId, Long receiverUserId, LocalDate pokeDate) {
        validate(groupChallengeId, activityRecordId, senderUserId, receiverUserId, pokeDate);
        return new Poke(groupChallengeId, activityRecordId, senderUserId, receiverUserId, pokeDate);
    }

    private static void validate(Long groupChallengeId, Long activityRecordId, Long senderUserId, Long receiverUserId, LocalDate pokeDate) {
        if (groupChallengeId == null) {
            throw new CustomException(PokeErrorCode.POKE_GROUP_CHALLENGE_REQUIRED);
        }

        if (activityRecordId == null) {
            throw new CustomException(PokeErrorCode.POKE_ACTIVITY_RECORD_REQUIRED);
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

        if (Objects.equals(senderUserId, receiverUserId)) {
            throw new CustomException(PokeErrorCode.POKE_SELF_NOT_ALLOWED);
        }
    }
}
