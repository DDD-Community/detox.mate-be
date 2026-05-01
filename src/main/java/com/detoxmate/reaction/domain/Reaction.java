package com.detoxmate.reaction.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.reaction.ReactionErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "reactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long id;

    @Column(name = "challenge_record_id", nullable = false)
    private Long challengeRecordId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "body", nullable = false)
    private ReactionBody body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    private Reaction(Long challengeRecordId, Long userId, ReactionBody body) {
        validateCreate(challengeRecordId, userId, body);

        this.challengeRecordId = challengeRecordId;
        this.userId = userId;
        this.body = body;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = null;
        this.deleted = false;
    }

    public static Reaction create(Long challengeRecordId, Long userId, ReactionBody body) {
        return new Reaction(challengeRecordId, userId, body);
    }

    public void deleteBy(Long currentUserId) {
        validateNotDeleted();
        validateAuthor(currentUserId);

        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateCreate(Long challengeRecordId, Long userId, ReactionBody body) {
        if (challengeRecordId == null) {
            throw new CustomException(ReactionErrorCode.REACTION_CHALLENGE_RECORD_REQUIRED);
        }

        if (userId == null) {
            throw new CustomException(ReactionErrorCode.REACTION_USER_REQUIRED);
        }

        if (body == null) {
            throw new CustomException(ReactionErrorCode.REACTION_BODY_REQUIRED);
        }
    }

    private void validateNotDeleted() {
        if (deleted) {
            throw new CustomException(ReactionErrorCode.REACTION_ALREADY_DELETED);
        }
    }

    private void validateAuthor(Long currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw new CustomException(ReactionErrorCode.REACTION_DELETE_FORBIDDEN);
        }
    }
}
