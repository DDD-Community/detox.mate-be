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
@Table(name = "`groups`")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group {

    public static final int MAX_NAME_LENGTH = 12;

    @Id
    @Column(name = "group_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 12)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 5)
    private String inviteCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Group(String name, String inviteCode) {
        validateName(name);
        this.name = name;
        this.inviteCode = inviteCode;
    }

    public static Group createNew(String name) {
        return new Group(name, null);
    }

    public static Group createNew(String name, String inviteCode) {
        return new Group(name, inviteCode);
    }

    private static void validateName(String name) {
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("그룹 이름은 12자 이하여야 합니다.");
        }
    }
}
