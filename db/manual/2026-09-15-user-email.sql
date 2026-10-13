-- DetoxMate user email / application schema migration / MySQL 8.4
-- Existing users keep a nullable email until the next verified social login.

ALTER TABLE users
    ADD COLUMN email VARCHAR(320) NULL,
    ADD UNIQUE KEY uk_users_email (email);
