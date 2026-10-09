-- Notification policy rollout, MySQL 8.x. STEP 1 OF 2: inspect / generate DDL.
-- Select the application database first. This file is READ-ONLY.
-- Run with the application stopped (including startup template initializers).
-- Save SHOW CREATE TABLE output and a backup of notification_type / notification.
-- Execute the generated schema_sql statements before the companion data script.
-- VARCHAR columns require no ENUM expansion; zero generated rows is normal for them.
-- Review generated DDL against SHOW CREATE TABLE before executing it. In particular,
-- stop if either column has EXTRA attributes or a CHECK constraint limiting values.
-- Generated string literals assume NO_BACKSLASH_ESCAPES is not enabled.
-- For the data step, sql_mode must include STRICT_TRANS_TABLES or STRICT_ALL_TABLES.
-- ALTER TABLE implicitly commits in MySQL; a later ROLLBACK cannot undo it.

SELECT DATABASE() AS selected_database, @@SESSION.sql_mode AS sql_mode;
SHOW CREATE TABLE notification_type;
SHOW CREATE TABLE notification;
SHOW CREATE TABLE notification_history;

-- Both duplicate checks must return zero rows. Resolve duplicates before continuing;
-- this migration intentionally does not delete or merge existing types/templates.
SELECT notification_type_name, COUNT(*) AS row_count
FROM notification_type
GROUP BY notification_type_name
HAVING COUNT(*) > 1;

SELECT notification_type_id, COUNT(*) AS template_count
FROM notification
GROUP BY notification_type_id
HAVING COUNT(*) > 1;

-- Both columns must exist, be ENUM or sufficiently sized VARCHAR, and have empty EXTRA.
-- VARCHAR minimum lengths: notification_type_name 28, target_type 27.
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE,
       IS_NULLABLE, COLUMN_DEFAULT, CHARACTER_SET_NAME, COLLATION_NAME, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'notification_type' AND COLUMN_NAME = 'notification_type_name')
    OR (TABLE_NAME = 'notification_history' AND COLUMN_NAME = 'target_type'));

-- Keep all existing ENUM members in their original order and append only missing ones.
-- Preserve charset, collation, nullability, default and comment.
-- These statements are generated, NOT automatically executed.
WITH required_values AS (
    SELECT 'notification_type' AS table_name, 'notification_type_name' AS column_name,
           1 AS sequence_no, 'FRIEND_UNLOCKED' AS enum_value
    UNION ALL SELECT 'notification_type', 'notification_type_name', 2, 'FRIEND_REQUEST_RECEIVED'
    UNION ALL SELECT 'notification_type', 'notification_type_name', 3, 'FRIEND_REQUEST_ACCEPTED'
    UNION ALL SELECT 'notification_type', 'notification_type_name', 4, 'FRIEND_APP_UNLOCKED'
    UNION ALL SELECT 'notification_type', 'notification_type_name', 5, 'FRIEND_TIME_LIMIT_CHANGED'
    UNION ALL SELECT 'notification_type', 'notification_type_name', 6, 'APP_RELOCK_REMINDER'
    UNION ALL SELECT 'notification_history', 'target_type', 1, 'APP_UNLOCK_TIMER'
    UNION ALL SELECT 'notification_history', 'target_type', 2, 'FRIEND_REQUESTS'
    UNION ALL SELECT 'notification_history', 'target_type', 3, 'FRIENDS'
), missing_values AS (
    SELECT c.TABLE_NAME, c.COLUMN_NAME,
           GROUP_CONCAT(QUOTE(r.enum_value) ORDER BY r.sequence_no SEPARATOR ',') AS members_to_add
    FROM information_schema.COLUMNS c
    JOIN required_values r
      ON r.table_name = c.TABLE_NAME AND r.column_name = c.COLUMN_NAME
    WHERE c.TABLE_SCHEMA = DATABASE()
      AND c.DATA_TYPE = 'enum'
      AND LOCATE(QUOTE(r.enum_value), c.COLUMN_TYPE) = 0
    GROUP BY c.TABLE_NAME, c.COLUMN_NAME
)
SELECT CONCAT(
    'ALTER TABLE `', c.TABLE_NAME, '` MODIFY COLUMN `', c.COLUMN_NAME, '` ',
    LEFT(c.COLUMN_TYPE, CHAR_LENGTH(c.COLUMN_TYPE) - 1), ',', m.members_to_add, ')',
    ' CHARACTER SET ', c.CHARACTER_SET_NAME, ' COLLATE ', c.COLLATION_NAME,
    IF(c.IS_NULLABLE = 'YES', ' NULL', ' NOT NULL'),
    IF(c.COLUMN_DEFAULT IS NULL, '', CONCAT(' DEFAULT ', QUOTE(c.COLUMN_DEFAULT))),
    ' COMMENT ', QUOTE(c.COLUMN_COMMENT), ';'
) AS schema_sql
FROM information_schema.COLUMNS c
JOIN missing_values m ON m.TABLE_NAME = c.TABLE_NAME AND m.COLUMN_NAME = c.COLUMN_NAME
WHERE c.TABLE_SCHEMA = DATABASE()
ORDER BY c.TABLE_NAME, c.COLUMN_NAME;

-- After applying the generated DDL, rerun this file: schema_sql must return zero rows.
-- Then run 2026-10-09-notification-policy-data.sql in the same selected database.
-- Rollback: retain added ENUM values, especially after new history rows exist.
-- Do not shrink the ENUM during an application rollback; it can invalidate stored data.
