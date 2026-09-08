CREATE TABLE apps (
    app_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    app_display_name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (app_id),
    KEY idx_apps_user_id (user_id),
    CONSTRAINT chk_apps_display_name
        CHECK (CHAR_LENGTH(TRIM(app_display_name)) BETWEEN 1 AND 100),
    CONSTRAINT fk_apps_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE app_time_limits (
    app_time_limit_id BIGINT NOT NULL AUTO_INCREMENT,
    app_id BIGINT NOT NULL,
    daily_limit_minutes INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (app_time_limit_id),
    UNIQUE KEY uk_app_time_limits_app_id (app_id),
    CONSTRAINT chk_app_time_limits_daily_limit
        CHECK (daily_limit_minutes BETWEEN 0 AND 1440),
    CONSTRAINT fk_app_time_limits_app
        FOREIGN KEY (app_id) REFERENCES apps (app_id) ON DELETE CASCADE
);
