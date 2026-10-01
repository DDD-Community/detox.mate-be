-- Apply before deploying the user-level time-limit API.
-- Each user owns one current setting; app-specific values are not backfilled.
CREATE TABLE time_limits (
    time_limit_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_lock_minutes INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (time_limit_id),
    UNIQUE KEY uk_time_limits_user_id (user_id),
    CONSTRAINT chk_time_limits_total_lock_minutes
        CHECK (total_lock_minutes BETWEEN 0 AND 1440),
    CONSTRAINT fk_time_limits_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
