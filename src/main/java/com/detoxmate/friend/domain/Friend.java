package com.detoxmate.friend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "friends")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FriendStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    private Friend(Long fromUserId, Long toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("친구 관계의 양쪽 사용자는 서로 달라야 합니다.");
        }

        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = FriendStatus.PENDING;
    }

    public static Friend request(Long fromUserId, Long toUserId) {
        return new Friend(fromUserId, toUserId);
    }

    public boolean isPending() {
        return status == FriendStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == FriendStatus.ACCEPTED;
    }

    public boolean isFrom(Long userId) {
        return fromUserId.equals(userId);
    }

    public boolean isTo(Long userId) {
        return toUserId.equals(userId);
    }

    public boolean involves(Long userId) {
        return isFrom(userId) || isTo(userId);
    }

    public Long otherUserId(Long userId) {
        if (isFrom(userId)) {
            return toUserId;
        }
        if (isTo(userId)) {
            return fromUserId;
        }
        throw new IllegalArgumentException("해당 사용자는 친구 관계에 포함되지 않습니다.");
    }
}
