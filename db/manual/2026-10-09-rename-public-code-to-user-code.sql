-- Five-character public-code schema rename, MySQL 8.x. Apply once during a coordinated cutover.
-- This is an alternative to 2026-10-09-user-code.sql; choose exactly one path.
-- Do not run both scripts sequentially.
-- Preconditions: users.public_code exists as nullable VARCHAR(5), and every non-NULL
-- value is a canonical five-character code; users.user_code
-- does not exist; uk_users_public_code is the unique index on public_code; and
-- uk_users_user_code does not exist. Inspect SHOW CREATE TABLE users and SHOW INDEX
-- FROM users before proceeding. If neither column exists, use the ADD script instead.
-- If user_code already exists or any column/index state differs, stop for manual
-- inspection. If both columns exist, do not auto-detect, merge, overwrite, or drop data.
-- The ten-character version was never deployed. This rename does not shorten or reissue
-- codes and must not be used for a ten-character column or values.
-- Stop public_code application writers and coordinate the schema/application cutover.
-- Do not run mixed public_code/user_code binary versions; start the matching user_code
-- application only after this migration and its verification complete.
-- Renaming preserves issued values, NULLs, the column definition, and uniqueness.
-- No backfill, code regeneration, or NOT NULL tightening is performed here.
ALTER TABLE users
    RENAME COLUMN public_code TO user_code,
    RENAME INDEX uk_users_public_code TO uk_users_user_code;

-- Verification (read-only; compare issued values and NULL count with pre-cutover snapshot):
-- SHOW CREATE TABLE users;
-- SHOW INDEX FROM users WHERE Key_name IN ('uk_users_public_code', 'uk_users_user_code');
-- SELECT COUNT(*) AS users_awaiting_backfill FROM users WHERE user_code IS NULL;
-- SELECT user_id, user_code FROM users ORDER BY user_id;
-- SELECT user_code, COUNT(*) FROM users WHERE user_code IS NOT NULL
-- GROUP BY user_code HAVING COUNT(*) > 1;
-- SELECT user_id, user_code FROM users WHERE user_code IS NOT NULL
-- AND (CHAR_LENGTH(user_code) <> 5
--      OR NOT REGEXP_LIKE(user_code, '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{5}$', 'c'));
