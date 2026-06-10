package com.detoxmate.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "social_login_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_login_users_provider_user", columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uk_social_login_users_user_provider", columnNames = {"user_id", "provider"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialLoginUser {

    @Id
    @Column(name = "social_login_user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "provider_refresh_token", length = 1000)
    private String providerRefreshToken;

    @Column(name = "provider_refresh_token_updated_at")
    private LocalDateTime providerRefreshTokenUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SocialLoginUser(User user, SocialProvider provider, String providerUserId) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public static SocialLoginUser link(User user, SocialProvider provider, String providerUserId) {
        return new SocialLoginUser(user, provider, providerUserId);
    }

    public void updateProviderRefreshToken(String providerRefreshToken) {
        this.providerRefreshToken = providerRefreshToken;
        this.providerRefreshTokenUpdatedAt = LocalDateTime.now();
    }
}
