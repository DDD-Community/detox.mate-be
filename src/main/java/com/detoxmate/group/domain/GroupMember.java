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
@Table(name = "group_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember {

    @Id
    @Column(name = "group_member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private GroupMember(Long userId, Long groupId, GroupMemberRole role, GroupMemberStatus status, LocalDateTime joinedAt) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role.name();
        this.status = status.name();
        this.joinedAt = joinedAt;
    }

    public static GroupMember createOwner(Long userId, Long groupId) {
        return new GroupMember(userId, groupId, GroupMemberRole.OWNER, GroupMemberStatus.ACTIVE, LocalDateTime.now());
    }

    public static GroupMember createMember(Long userId, Long groupId) {
        return new GroupMember(userId, groupId, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE, LocalDateTime.now());
    }
}
