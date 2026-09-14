-- DetoxMate friends v2 / MVP design draft / MySQL 8.4
-- Reference DDL only: not an automatic migration or existing-data conversion.
-- Prerequisite: users(user_id BIGINT PRIMARY KEY), InnoDB.
-- Design notes: friends-v2.md

CREATE TABLE friend_invites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_friend_invites_user (user_id),
    UNIQUE KEY uk_friend_invites_code (code),
    CONSTRAINT fk_friend_invites_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE friends (
    id BIGINT NOT NULL AUTO_INCREMENT,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    accepted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    -- Preserve request direction; only the uniqueness key normalizes the pair.
    UNIQUE KEY uk_friends_pair ((LEAST(from_user_id, to_user_id)), (GREATEST(from_user_id, to_user_id))),
    KEY idx_friends_from_status (from_user_id, status),
    KEY idx_friends_to_status (to_user_id, status),
    CONSTRAINT fk_friends_from FOREIGN KEY (from_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_friends_to FOREIGN KEY (to_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_friends_not_self CHECK (from_user_id <> to_user_id),
    CONSTRAINT ck_friends_status CHECK (status IN ('PENDING', 'ACCEPTED')),
    CONSTRAINT ck_friends_accepted_at CHECK (
        (status = 'PENDING' AND accepted_at IS NULL)
        OR (status = 'ACCEPTED' AND accepted_at IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
