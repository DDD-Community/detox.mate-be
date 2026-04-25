package com.detoxmate.activityrecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "activity_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityRecord {

    @Id
    @Column(name = "activity_record_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_challenge_participant_id", nullable = false)
    private Long groupChallengeParticipantId;

    @Column(name = "activity_image_url", length = 500)
    private String activityImageUrl;

    @Lob
    @Column(name = "reflection_text")
    private String reflectionText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ActivityRecord(Long userId, Long groupChallengeParticipantId, String activityImageUrl, String reflectionText) {
        this.userId = userId;
        this.groupChallengeParticipantId = groupChallengeParticipantId;
        this.activityImageUrl = activityImageUrl;
        this.reflectionText = reflectionText;
    }

    public static ActivityRecord create(
            Long userId,
            Long groupChallengeParticipantId,
            String activityImageUrl,
            String reflectionText
    ) {
        return new ActivityRecord(userId, groupChallengeParticipantId, activityImageUrl, reflectionText);
    }
}
