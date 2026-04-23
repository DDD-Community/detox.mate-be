package com.detoxmate.group.domain;

import com.detoxmate.group.service.InviteCodeGenerator;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "`groups`")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group {

    public static final int MAX_NAME_LENGTH = 12;
    private static final int MAX_INVITE_CODE_GENERATION_ATTEMPTS = 10;

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

    public static Group createNew(String name, InviteCodeGenerator inviteCodeGenerator) {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_GENERATION_ATTEMPTS; attempt++) {
            String inviteCode = inviteCodeGenerator.generate();

            try {
                return new Group(name, inviteCode);
            } catch (DataIntegrityViolationException exception) {
                // `invite_code` 유일성은 DB가 보장하므로, 중복 충돌이 나면 여기서 재시도한다.
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용 가능한 초대코드를 생성할 수 없습니다.");
    }

    private static void validateName(String name) {
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("그룹 이름은 12자 이하여야 합니다.");
        }
    }

    public void updateOwner(Long userId) {
        GroupMember owner = GroupMember.createOwner(userId, this.id);
    }
}
