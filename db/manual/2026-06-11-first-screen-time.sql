CREATE TABLE first_screen_time (
    first_screen_time_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    group_challenge_participant_id BIGINT NOT NULL,
    total_used_minutes INT NOT NULL,
    record_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (first_screen_time_id),
    UNIQUE KEY uk_first_screen_time_participant (group_challenge_participant_id),
    KEY idx_first_screen_time_user_id (user_id)
);
