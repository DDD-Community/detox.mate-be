package com.detoxmate.activityrecordchallengestatus.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "activity_record_challenge_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityRecordChallengeStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_record_challenge_status_id")
    private int id;

    @Column(name = "group_challenge_id", nullable = false)
    private Long groupChallengeId;

    @Column(name = "activity_record_id", nullable = false)
    private Long activityRecordId;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "reaction_count", nullable = false)
    private int reactionCount;

    @Column(name = "poke_count", nullable = false)
    private int pokeCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ActivityRecordChallengeStatus(Long groupChallengeId, Long activityRecordId) {
        this.groupChallengeId = groupChallengeId;
        this.activityRecordId = activityRecordId;
        this.commentCount = 0;
        this.reactionCount = 0;
        this.pokeCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public static ActivityRecordChallengeStatus create(Long groupChallengeId, Long activityRecordId) {
        return new ActivityRecordChallengeStatus(groupChallengeId, activityRecordId);
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void increaseReactionCount() {
        this.reactionCount++;
    }

    public void increasePokeCount() {
        this.pokeCount++;
    }
}
