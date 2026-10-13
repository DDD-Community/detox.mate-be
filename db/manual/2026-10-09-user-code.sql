-- Five-character user-code rollout, MySQL 8.x. Apply once before deploying code-issuing signup.
-- The ten-character version was never deployed; this script does not convert existing codes.
-- Choose exactly one migration path; do not run this and the rename script sequentially.
-- Preconditions: users has neither public_code nor user_code, and neither
-- uk_users_public_code nor uk_users_user_code exists. Inspect SHOW CREATE TABLE users first.
-- If public_code and uk_users_public_code already exist, use
-- 2026-10-09-rename-public-code-to-user-code.sql instead.
-- If user_code already exists, or column/index state differs from either documented path,
-- stop for manual inspection. In particular, never auto-merge two existing code columns.
-- Coordinate the schema/application cutover: stop any public_code application writers
-- and do not run mixed public_code/user_code binary versions.
-- Additive stage: existing users stay NULL and can continue to log in.
-- MySQL unique keys allow multiple NULL values. No backfill is performed here.
ALTER TABLE users
    ADD COLUMN user_code VARCHAR(5) NULL,
    ADD UNIQUE KEY uk_users_user_code (user_code);

-- Verification (read-only):
-- SHOW CREATE TABLE users;
-- SHOW INDEX FROM users WHERE Key_name = 'uk_users_user_code';
-- SELECT COUNT(*) AS users_awaiting_backfill FROM users WHERE user_code IS NULL;
-- SELECT user_code, COUNT(*) FROM users WHERE user_code IS NOT NULL
-- GROUP BY user_code HAVING COUNT(*) > 1;
-- SELECT user_id, user_code FROM users WHERE user_code IS NOT NULL
-- AND (CHAR_LENGTH(user_code) <> 5
--      OR NOT REGEXP_LIKE(user_code, '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{5}$', 'c'));

-- Later, separately approved stage only: backfill canonical, unique codes with collision retries,
-- verify zero NULL/invalid/duplicate values, and ensure all running writers issue codes.
-- Then a later deployment may set the entity column to nullable=false and apply:
-- ALTER TABLE users MODIFY COLUMN user_code VARCHAR(5) NOT NULL;
-- Do not run this tightening until backfill is complete; legacy login has no backfill gate.
