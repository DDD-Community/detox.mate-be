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

    @Column(name = "activity_record_id", nullable = false)
    private Long activityRecordId;

    @Column(name = "group_challenge_id", nullable = false)
    private Long groupChallengeId;

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

    private Reaction(Long activityRecordId, Long groupChallengeId, Long userId, ReactionBody body) {
        this.activityRecordId = activityRecordId;
        this.groupChallengeId = groupChallengeId;
        this.userId = userId;
        this.body = body;
        this.createdAt = LocalDateTime.now();
        this.deleted = false;
    }

    public static Reaction create(Long activityRecordId, Long groupChallengeId, Long userId, ReactionBody body) {
        validateBody(body);
        return new Reaction(activityRecordId, groupChallengeId, userId, body);
    }

    public void deleteBy(Long currentUserId) {
        if (deleted) {
            throw new CustomException(ReactionErrorCode.REACTION_ALREADY_DELETED);
        }

        if (!Objects.equals(userId, currentUserId)) {
            throw new CustomException(ReactionErrorCode.REACTION_DELETE_FORBIDDEN);
        }

        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateBody(ReactionBody body) {
        if (body == null) {
            throw new CustomException(ReactionErrorCode.REACTION_BODY_REQUIRED);
        }
    }
}
