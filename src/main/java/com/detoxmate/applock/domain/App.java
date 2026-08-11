package com.detoxmate.applock.domain;

import com.detoxmate.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "apps")
@Check(constraints = "CHAR_LENGTH(TRIM(app_display_name)) BETWEEN 1 AND 100")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class App {

    @Id
    @Column(name = "app_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "app_display_name", nullable = false, length = 100)
    private String appDisplayName;

    @OneToOne(mappedBy = "app", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private AppTimeLimit appTimeLimit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private App(User user, String appDisplayName, Integer dailyLimitMinutes) {
        validateUser(user);
        validateAppDisplayName(appDisplayName);
        this.user = user;
        this.appDisplayName = appDisplayName;
        this.appTimeLimit = AppTimeLimit.create(this, dailyLimitMinutes);
    }

    public static App create(User user, String appDisplayName, Integer dailyLimitMinutes) {
        return new App(user, appDisplayName, dailyLimitMinutes);
    }

    public void update(String appDisplayName, Integer dailyLimitMinutes) {
        validateAppDisplayName(appDisplayName);
        AppTimeLimit.validateDailyLimitMinutes(dailyLimitMinutes);
        this.appDisplayName = appDisplayName;
        this.appTimeLimit.changeDailyLimitMinutes(dailyLimitMinutes);
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user는 필수입니다.");
        }
    }

    private static void validateAppDisplayName(String appDisplayName) {
        if (appDisplayName == null || appDisplayName.isBlank()) {
            throw new IllegalArgumentException("appDisplayName은 필수입니다.");
        }

        if (appDisplayName.length() > 100) {
            throw new IllegalArgumentException("appDisplayName은 100자 이하여야 합니다.");
        }
    }
}
