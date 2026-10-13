package com.detoxmate.applock.domain;

import com.detoxmate.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "time_limits")
@Check(constraints = "total_lock_minutes BETWEEN 0 AND 1440")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeLimit {

    @Id
    @Column(name = "time_limit_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "total_lock_minutes", nullable = false)
    private Integer totalLockMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private TimeLimit(User user, Integer totalLockMinutes) {
        if (user == null) {
            throw new IllegalArgumentException("user는 필수입니다.");
        }
        validateTotalLockMinutes(totalLockMinutes);
        this.user = user;
        this.totalLockMinutes = totalLockMinutes;
    }

    public static TimeLimit create(User user, Integer totalLockMinutes) {
        return new TimeLimit(user, totalLockMinutes);
    }

    public boolean changeTotalLockMinutes(Integer totalLockMinutes) {
        validateTotalLockMinutes(totalLockMinutes);
        if (this.totalLockMinutes.equals(totalLockMinutes)) {
            return false;
        }
        this.totalLockMinutes = totalLockMinutes;
        return true;
    }

    private static void validateTotalLockMinutes(Integer totalLockMinutes) {
        if (totalLockMinutes == null || totalLockMinutes < 0 || totalLockMinutes > 1440) {
            throw new IllegalArgumentException("totalLockMinutes는 0 이상 1440 이하여야 합니다.");
        }
    }
}
