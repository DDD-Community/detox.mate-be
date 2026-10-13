-- Notification policy rollout, MySQL 8.x. STEP 2 OF 2: types / templates.
-- First complete 2026-10-09-notification-policy-schema.sql and its prechecks.
-- Use a UTF-8 connection and select the application database before running this file.
-- Preconditions: InnoDB tables, no duplicate type names or templates per type,
-- and exactly one migration writer with all application instances stopped.
-- sql_mode must include STRICT_TRANS_TABLES or STRICT_ALL_TABLES.
-- Run the whole file in one session. Stop on the first error and ROLLBACK;
-- do not use mysql --force / a client that continues and commits after an error.
-- No ID values are hardcoded. Existing type IDs 1..12 and template IDs stay unchanged.
-- Serial reruns are supported; no unique constraint on type name is assumed.
-- This changes future notification templates only, NOT notification_history snapshots.

SET NAMES utf8mb4;

CREATE TEMPORARY TABLE notification_policy_20261009_templates (
    sequence_no INT NOT NULL,
    type_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    message_template VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL
) ENGINE = InnoDB;

-- Match NotificationTemplateInitializer exactly, including retained legacy templates.
INSERT INTO notification_policy_20261009_templates (sequence_no, type_code, message_template)
VALUES
    (1, 'GROUP_JOINED', '{nickname}님이 {groupName}에 합류했어요. 확인해보세요!'),
    (2, 'CERTIFICATION_CREATED', '{nickname}님이 인증을 업로드했어요. 반응을 남겨보세요!'),
    (3, 'POKE_RECEIVED', '{nickname}님이 {me}님을 콕 찔렀어요. 인증하러 가볼까요?'),
    (4, 'REACTION_CREATED', '{nickname}님이 반응을 남겼어요. {me}님도 반응을 보내볼까요?'),
    (5, 'COMMENT_CREATED', '{nickname}님이 댓글을 남겼어요: "{commentBody}"'),
    (6, 'STREAK_WARNING', '{remainingCount}명이 더 인증하지 않으면 우리 그룹 스트릭이 깨져요!'),
    (7, 'DAILY_CERTIFICATION_REMINDER', '아직 오늘의 인증을 안 했어요. 인증하러 가볼까요?'),
    (8, 'WEEKLY_GOAL_SUMMARY', '이번 주는 {achievementCount}번 목표 달성을 했네요! 다음 주도 파이팅!'),
    (9, 'CERTIFICATION_START_TOMORROW', '내일부터 {groupName} 인증이 시작돼요. 오늘부터 디톡스를 시작해요!'),
    (10, 'GOAL_SETTING_REMINDER', '{nickname}님의 목표 설정을 멤버들이 기다리고 있어요. 목표 설정하러 가볼까요?'),
    (11, 'POKE_GOAL_SETTING_REMINDER', '{nickname}님이 {me}님을 콕 찔렀어요. 목표 설정을 해볼까요?'),
    (12, 'APP_UNLOCK_REQUESTED', '앱을 사용하려면, 이 알림을 클릭해주세요!'),
    (13, 'FRIEND_UNLOCKED', '{friendName}님이 잠근 앱을 등록 해제했어요.'),
    (14, 'FRIEND_REQUEST_RECEIVED', '{friendName}님이 친구 요청을 보냈어요.'),
    (15, 'FRIEND_REQUEST_ACCEPTED', '{friendName}님과 친구가 되었어요! 함께 스크린타임을 줄여봐요.'),
    (16, 'FRIEND_APP_UNLOCKED', '{friendName}님이 잠긴 앱을 {minutes}분 동안 일시 해제했어요.'),
    (17, 'FRIEND_TIME_LIMIT_CHANGED', '{friendName}님이 목표 제한 시간을 {duration}으로 변경했어요.'),
    (18, 'APP_RELOCK_REMINDER', '앱 사용 시간이 얼마 남지 않았어요!');

START TRANSACTION;

-- Six missing types are inserted for the supplied 12-row baseline.
-- sequence_no only orders inserts; it is NOT a prescribed database ID.
INSERT INTO notification_type (notification_type_name)
SELECT s.type_code
FROM notification_policy_20261009_templates s
LEFT JOIN notification_type t ON t.notification_type_name = s.type_code
WHERE t.notification_type_id IS NULL
ORDER BY s.sequence_no;

-- Keep existing notification IDs so history foreign keys remain valid.
UPDATE notification n
JOIN notification_type t ON t.notification_type_id = n.notification_type_id
JOIN notification_policy_20261009_templates s ON s.type_code = t.notification_type_name
SET n.notification_title = 'Detoxmate',
    n.notification_message_template = s.message_template;

INSERT INTO notification (notification_type_id, notification_title, notification_message_template)
SELECT t.notification_type_id, 'Detoxmate', s.message_template
FROM notification_policy_20261009_templates s
JOIN notification_type t ON t.notification_type_name = s.type_code
LEFT JOIN notification n ON n.notification_type_id = t.notification_type_id
WHERE n.notification_id IS NULL
ORDER BY s.sequence_no;

COMMIT;

-- Verification: 18 rows, one type/template per code, all titles Detoxmate.
SELECT t.notification_type_id, t.notification_type_name,
       n.notification_id, n.notification_title, n.notification_message_template
FROM notification_policy_20261009_templates s
JOIN notification_type t ON t.notification_type_name = s.type_code
JOIN notification n ON n.notification_type_id = t.notification_type_id
ORDER BY s.sequence_no;

-- Verification: must return zero rows.
SELECT s.type_code
FROM notification_policy_20261009_templates s
LEFT JOIN notification_type t ON t.notification_type_name = s.type_code
LEFT JOIN notification n ON n.notification_type_id = t.notification_type_id
WHERE n.notification_id IS NULL
   OR BINARY n.notification_title <> BINARY 'Detoxmate'
   OR BINARY n.notification_message_template <> BINARY s.message_template;

DROP TEMPORARY TABLE notification_policy_20261009_templates;

-- Retired scheduler types/templates remain for history/FK compatibility. Their presence
-- does not schedule notifications; deploy the application with the scheduler removed.
-- Application rollback: retain new types/templates and ENUM members. Deleting them can
-- break history references. Restore previous template text from the pre-migration backup
-- only if required by the rollback version; no history deletion/backfill is part of this script.
