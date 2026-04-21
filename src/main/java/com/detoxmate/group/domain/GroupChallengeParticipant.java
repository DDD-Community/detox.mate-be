package com.detoxmate.group.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "group_challenge_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupChallengeParticipant {

    @Id
    @Column(name = "group_challenge_participant_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_challenge_id", nullable = false)
    private Long groupChallengeId;

    @Column(name = "group_member_id", nullable = false)
    private Long groupMemberId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "baseline_screen_time_minutes")
    private Integer baselineScreenTimeMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private GroupChallengeParticipant(Long groupMemberId, Long groupChallengeId, GroupChallengeParticipantStatus status) {
        this.groupChallengeId = groupChallengeId;
        this.groupMemberId = groupMemberId;
        this.status = status.name();
        this.joinedAt = LocalDateTime.now();
    }

    public static GroupChallengeParticipant join(Long groupMemberId, Long groupChallengeId) {
        return new GroupChallengeParticipant(groupMemberId, groupChallengeId, GroupChallengeParticipantStatus.JOINED);
    }

    public void withdraw() {
        if (!GroupChallengeParticipantStatus.JOINED.name().equals(status)) {
            return;
        }

        status = GroupChallengeParticipantStatus.WITHDRAWN.name();
        withdrawnAt = LocalDateTime.now();
    }
}
