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
@Table(name = "group_challenges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupChallenge {

    @Id
    @Column(name = "group_challenge_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "challenge_no", nullable = false)
    private Integer challengeNo;

    @Column(name = "status", nullable = false, length = 20)
    private GroupChallengeStatus status;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private GroupChallenge(Long groupId, Integer challengeNo, GroupChallengeStatus status) {
        this.groupId = groupId;
        this.challengeNo = challengeNo;
        this.status = GroupChallengeStatus.valueOf(status.name());
    }

    public static GroupChallenge createFirst(Long groupId) {
        return new GroupChallenge(groupId, 1, GroupChallengeStatus.RECRUITING);
    }
}
