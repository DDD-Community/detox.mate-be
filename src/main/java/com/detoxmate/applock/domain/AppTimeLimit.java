package com.detoxmate.applock.domain;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "app_time_limits")
@Check(constraints = "daily_limit_minutes BETWEEN 0 AND 1440")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppTimeLimit {

    @Id
    @Column(name = "app_time_limit_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_id", nullable = false, unique = true)
    private App app;

    @Column(name = "daily_limit_minutes", nullable = false)
    private Integer dailyLimitMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private AppTimeLimit(App app, Integer dailyLimitMinutes) {
        validateApp(app);
        validateDailyLimitMinutes(dailyLimitMinutes);
        this.app = app;
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    static AppTimeLimit create(App app, Integer dailyLimitMinutes) {
        return new AppTimeLimit(app, dailyLimitMinutes);
    }

    void changeDailyLimitMinutes(Integer dailyLimitMinutes) {
        validateDailyLimitMinutes(dailyLimitMinutes);
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    private static void validateApp(App app) {
        if (app == null) {
            throw new IllegalArgumentException("app은 필수입니다.");
        }
    }

    static void validateDailyLimitMinutes(Integer dailyLimitMinutes) {
        if (dailyLimitMinutes == null) {
            throw new IllegalArgumentException("dailyLimitMinutes는 필수입니다.");
        }

        if (dailyLimitMinutes < 0 || dailyLimitMinutes > 1440) {
            throw new IllegalArgumentException("dailyLimitMinutes는 0 이상 1440 이하여야 합니다.");
        }
    }
}
