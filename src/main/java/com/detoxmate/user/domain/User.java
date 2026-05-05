package com.detoxmate.user.domain;

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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

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

    private User(String displayName, String profileImageObjectKey) {
        this.displayName = displayName;
        this.profileImageObjectKey = profileImageObjectKey;
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
}
