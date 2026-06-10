ALTER TABLE social_login_users
    ADD COLUMN provider_refresh_token VARCHAR(1000) NULL COMMENT 'Apple refresh token encrypted for revoke',
    ADD COLUMN provider_refresh_token_updated_at DATETIME(6) NULL;
