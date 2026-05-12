package com.detoxmate.user.domain;

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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public static final String WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자";

    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 30)
    private String displayName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "profile_image_object_key", length = 1024)
    private String profileImageObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private UserRole role;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    private User(String displayName, String profileImageObjectKey) {
        this.displayName = displayName;
        this.profileImageObjectKey = profileImageObjectKey;
        this.status = UserStatus.ACTIVE;
        this.role = UserRole.USER;
    }

    public static User createNew(String displayName) {
        return createNew(displayName, null);
    }

    public static User createNew(String displayName, String profileImageObjectKey) {
        return new User(displayName, profileImageObjectKey);
    }

    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void changeProfileImageObjectKey(String profileImageObjectKey) {
        this.profileImageObjectKey = profileImageObjectKey;
    }

    public boolean isActive() {
        return status == null || status == UserStatus.ACTIVE;
    }

    public boolean isWithdrawn() {
        return status == UserStatus.WITHDRAWN;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public void grantAdminRole() {
        role = UserRole.ADMIN;
    }

    public String getPublicDisplayName() {
        if (isWithdrawn()) {
            return WITHDRAWN_DISPLAY_NAME;
        }

        return displayName;
    }

    public String getPublicProfileImageObjectKey() {
        if (isWithdrawn()) {
            return null;
        }

        return profileImageObjectKey;
    }

    public void withdraw() {
        if (isWithdrawn()) {
            return;
        }

        status = UserStatus.WITHDRAWN;
        withdrawnAt = LocalDateTime.now();
        displayName = WITHDRAWN_DISPLAY_NAME;
        profileImageObjectKey = null;
    }
}
