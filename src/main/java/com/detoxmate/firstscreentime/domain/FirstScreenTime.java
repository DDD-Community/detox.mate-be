package com.detoxmate.firstscreentime.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "first_screen_time",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_first_screen_time_participant",
                columnNames = "group_challenge_participant_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FirstScreenTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "first_screen_time_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_challenge_participant_id", nullable = false)
    private Long groupChallengeParticipantId;

    @Column(name = "total_used_minutes", nullable = false)
    private Integer screenTimeMinutes;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private FirstScreenTime(
            Long userId,
            Long groupChallengeParticipantId,
            Integer screenTimeMinutes,
            LocalDate recordDate
    ) {
        this.userId = userId;
        this.groupChallengeParticipantId = groupChallengeParticipantId;
        this.screenTimeMinutes = screenTimeMinutes;
        this.recordDate = recordDate;
    }

    public static FirstScreenTime create(
            Long userId,
            Long groupChallengeParticipantId,
            Integer screenTimeMinutes,
            LocalDate recordDate
    ) {
        return new FirstScreenTime(userId, groupChallengeParticipantId, screenTimeMinutes, recordDate);
    }
}
