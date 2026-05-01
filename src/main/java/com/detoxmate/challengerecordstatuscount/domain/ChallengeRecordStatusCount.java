package com.detoxmate.challengerecordstatuscount.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecordstatuscount.ChallengeRecordStatusCountErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "challenge_record_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeRecordStatusCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_record_status_id")
    private Long id;

    @Column(name = "challenge_record_id", nullable = false)
    private Long challengeRecordId;

    @Column(name = "before_comment_count", nullable = false)
    private int beforeCommentCount;

    @Column(name = "after_comment_count", nullable = false)
    private int afterCommentCount;

    @Column(name = "reaction_count", nullable = false)
    private int reactionCount;

    @Column(name = "poke_count", nullable = false)
    private int pokeCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ChallengeRecordStatusCount(Long challengeRecordId) {
        validate(challengeRecordId);

        this.challengeRecordId = challengeRecordId;
        this.beforeCommentCount = 0;
        this.afterCommentCount = 0;
        this.reactionCount = 0;
        this.pokeCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public static ChallengeRecordStatusCount create(Long challengeRecordId) {
        return new ChallengeRecordStatusCount(challengeRecordId);
    }

    public void increaseBeforeCommentCount() {
        beforeCommentCount++;
    }

    public void increaseAfterCommentCount() {
        afterCommentCount++;
    }

    public void increaseReactionCount() {
        reactionCount++;
    }

    public void increasePokeCount() {
        pokeCount++;
    }

    private static void validate(Long challengeRecordId) {
        if (challengeRecordId == null) {
            throw new CustomException(ChallengeRecordStatusCountErrorCode.CHALLENGE_RECORD_REQUIRED);
        }
    }
}
