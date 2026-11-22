-- Fresh MySQL 8.4 schema for Detoxmate origin/dev ab13470c9560844adeaf52591cfa89845bea51f4.
-- Generated from a disposable MySQL 8.4 database bootstrapped by Hibernate 7,
-- then supplemented with constraints from db/manual SQL.
-- Run only after selecting a NEW, EMPTY application database. This script does not
-- drop/create the database or preserve any existing data.
-- Do not also run historical incremental ADD/RENAME/DROP scripts on this schema.

SELECT DATABASE() AS selected_database;
SET NAMES utf8mb4;
SET @detoxmate_old_fk_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;
CREATE TABLE `activity_record` (
  `activity_record_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `group_challenge_participant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `activity_image_object_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reflection_text` tinytext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`activity_record_id`),
  KEY `FKkgb3iqwr64mf21uphnev3fvyc` (`group_challenge_participant_id`),
  KEY `FKfrbmh3ykgpea6e8mst0loy6n` (`user_id`),
  CONSTRAINT `FKfrbmh3ykgpea6e8mst0loy6n` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKkgb3iqwr64mf21uphnev3fvyc` FOREIGN KEY (`group_challenge_participant_id`) REFERENCES `group_challenge_participants` (`group_challenge_participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `activity_record_detail` (
  `is_achieved` bit(1) NOT NULL,
  `use_minutes` int NOT NULL,
  `activity_record_detail_id` bigint NOT NULL AUTO_INCREMENT,
  `activity_record_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `usage_goal_type_id` bigint NOT NULL,
  `user_usage_goal_times_id` bigint NOT NULL,
  PRIMARY KEY (`activity_record_detail_id`),
  UNIQUE KEY `UK_ACTIVITY_RECORD_DETAIL_RECORD_GOAL_TYPE` (`activity_record_id`,`usage_goal_type_id`),
  KEY `FKhvdmjqtly9msb216jd3tjo0uk` (`usage_goal_type_id`),
  KEY `FK8gyh4e83k7360yky3bvyr4rm1` (`user_usage_goal_times_id`),
  CONSTRAINT `FK8gyh4e83k7360yky3bvyr4rm1` FOREIGN KEY (`user_usage_goal_times_id`) REFERENCES `user_usage_goal_times` (`user_usage_goal_times_id`),
  CONSTRAINT `FKelhs6acxs2gnavj0d8ntqgjys` FOREIGN KEY (`activity_record_id`) REFERENCES `activity_record` (`activity_record_id`),
  CONSTRAINT `FKhvdmjqtly9msb216jd3tjo0uk` FOREIGN KEY (`usage_goal_type_id`) REFERENCES `usage_goal_type` (`usage_goal_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `challenge_record` (
  `record_date` date NOT NULL,
  `activity_record_id` bigint DEFAULT NULL,
  `challenge_record_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `group_challenge_id` bigint NOT NULL,
  `group_challenge_participant_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `status` enum('AFTER_RECORD_FAIL','AFTER_RECORD_SUCCESS','BEFORE_RECORD') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`challenge_record_id`),
  UNIQUE KEY `uk_challenge_record_participant_date` (`group_challenge_id`,`group_challenge_participant_id`,`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `challenge_record_status` (
  `after_comment_count` int NOT NULL,
  `before_comment_count` int NOT NULL,
  `poke_count` int NOT NULL,
  `reaction_count` int NOT NULL,
  `challenge_record_id` bigint NOT NULL,
  `challenge_record_status_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`challenge_record_status_id`),
  UNIQUE KEY `uk_challenge_record_status_record` (`challenge_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comments` (
  `challenge_record_id` bigint NOT NULL,
  `comment_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `comment_body` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `comment_status` enum('AFTER_RECORD','BEFORE_RECORD') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FCM tokens allow 4096 UTF-8 characters, which exceeds MySQL's full index
-- key limit. Keep exact token lookup on the original column and enforce
-- practical uniqueness through its indexed SHA-256 digest. NO PAD binary
-- collation keeps token lookup consistent for values with trailing spaces.
CREATE TABLE `fcm_token` (
  `created_at` datetime(6) NOT NULL,
  `fcm_token_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `token` varchar(4096) COLLATE utf8mb4_0900_bin NOT NULL,
  `token_sha256` binary(32) GENERATED ALWAYS AS (UNHEX(SHA2(`token`, 256))) STORED,
  `platform` enum('ANDROID','IOS','WEB') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`fcm_token_id`),
  UNIQUE KEY `uk_fcm_token_token_sha256` (`token_sha256`),
  KEY `idx_fcm_token_user_id` (`user_id`),
  KEY `idx_fcm_token_token_prefix` (`token`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `first_screen_time` (
  `record_date` date NOT NULL,
  `total_used_minutes` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `first_screen_time_id` bigint NOT NULL AUTO_INCREMENT,
  `group_challenge_participant_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`first_screen_time_id`),
  UNIQUE KEY `uk_first_screen_time_participant` (`group_challenge_participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `friend_invites` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKd4v65hc5sklemgchilekx093k` (`user_id`),
  UNIQUE KEY `UKhiqgbgmrj22wuhfymhu09042b` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `friends` (
  `accepted_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `from_user_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `to_user_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `status` enum('ACCEPTED','PENDING') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `group_challenge_participants` (
  `baseline_screen_time_minutes` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `group_challenge_id` bigint NOT NULL,
  `group_challenge_participant_id` bigint NOT NULL AUTO_INCREMENT,
  `group_member_id` bigint NOT NULL,
  `joined_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `withdrawn_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`group_challenge_participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `group_challenges` (
  `challenge_no` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `end_at` datetime(6) DEFAULT NULL,
  `group_challenge_id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `start_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`group_challenge_id`),
  CONSTRAINT `group_challenges_chk_1` CHECK ((`status` in (_utf8mb4'RECRUITING',_utf8mb4'ACTIVE',_utf8mb4'COMPLETED',_utf8mb4'CANCELED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `group_members` (
  `created_at` datetime(6) NOT NULL,
  `group_id` bigint NOT NULL,
  `group_member_id` bigint NOT NULL AUTO_INCREMENT,
  `joined_at` datetime(6) NOT NULL,
  `left_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`group_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `groups` (
  `invite_code` varchar(5) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `group_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `name` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`group_id`),
  UNIQUE KEY `UKosxxb9ssw4g78s0cgtistpeqg` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `notification_type_id` bigint NOT NULL,
  `notification_title` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notification_message_template` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `FK3x921lcnkybqyh7pqeg9u2x7j` (`notification_type_id`),
  CONSTRAINT `FK3x921lcnkybqyh7pqeg9u2x7j` FOREIGN KEY (`notification_type_id`) REFERENCES `notification_type` (`notification_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification_history` (
  `is_read` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) DEFAULT NULL,
  `notification_history_id` bigint NOT NULL AUTO_INCREMENT,
  `notification_id` bigint NOT NULL,
  `sender_user_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `notification_history_title` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notification_history_message` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` enum('CHALLENGE_RECORD','COMMENT','NONE','POKE','REACTION') COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` enum('APP_UNLOCK_DURATION_SETTING','APP_UNLOCK_TIMER','FEED','FEED_DETAIL','FRIENDS','FRIEND_REQUESTS','GROUP','GROUP_CHALLENGE','MY_PAGE','NONE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`notification_history_id`),
  KEY `FK5or7xfrp7v3y16p174fa7tjfq` (`notification_id`),
  CONSTRAINT `FK5or7xfrp7v3y16p174fa7tjfq` FOREIGN KEY (`notification_id`) REFERENCES `notification` (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification_type` (
  `notification_type_id` bigint NOT NULL AUTO_INCREMENT,
  `notification_type_name` enum('APP_RELOCK_REMINDER','APP_UNLOCK_REQUESTED','CERTIFICATION_CREATED','CERTIFICATION_START_TOMORROW','COMMENT_CREATED','DAILY_CERTIFICATION_REMINDER','FRIEND_APP_UNLOCKED','FRIEND_REQUEST_ACCEPTED','FRIEND_REQUEST_RECEIVED','FRIEND_TIME_LIMIT_CHANGED','FRIEND_UNLOCKED','GOAL_SETTING_REMINDER','GROUP_JOINED','POKE_GOAL_SETTING_REMINDER','POKE_RECEIVED','REACTION_CREATED','STREAK_WARNING','WEEKLY_GOAL_SUMMARY') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`notification_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pokes` (
  `poke_date` date NOT NULL,
  `challenge_record_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `poke_id` bigint NOT NULL AUTO_INCREMENT,
  `receiver_user_id` bigint NOT NULL,
  `sender_user_id` bigint NOT NULL,
  PRIMARY KEY (`poke_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reactions` (
  `is_deleted` bit(1) NOT NULL,
  `challenge_record_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `reaction_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `body` enum('CLAP','FIRE','HAMMER','LIKED','MUSCLE','SURPRISED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`reaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_token_session` (
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `last_used_at` datetime(6) NOT NULL,
  `refresh_token_session_id` bigint NOT NULL AUTO_INCREMENT,
  `revoked_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`refresh_token_session_id`),
  UNIQUE KEY `uk_refresh_token_session_token_hash` (`token_hash`),
  KEY `idx_refresh_token_session_user_id` (`user_id`),
  CONSTRAINT `FKko6drq0r2r6k7wrgaq71ps5c1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `screen_time_ocr_error_report` (
  `corrected_total_used_minutes` int DEFAULT NULL,
  `ocr_total_used_minutes` int NOT NULL,
  `record_date` date NOT NULL,
  `activity_record_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `group_challenge_participant_id` bigint NOT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `screen_time_ocr_error_report_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `image_object_key` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL,
  `admin_note` text COLLATE utf8mb4_unicode_ci,
  `status` enum('CORRECTED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`screen_time_ocr_error_report_id`),
  KEY `idx_screen_time_ocr_report_status_created_at` (`status`,`created_at`,`screen_time_ocr_error_report_id`),
  KEY `idx_screen_time_ocr_report_participant_record_date` (`group_challenge_participant_id`,`record_date`),
  KEY `idx_screen_time_ocr_report_activity_record` (`activity_record_id`),
  CONSTRAINT `FK10fm2dj3iqe96djm12qitalx5` FOREIGN KEY (`activity_record_id`) REFERENCES `activity_record` (`activity_record_id`),
  CONSTRAINT `FKjetf0ejm3qep7wyn2i5wwwvpc` FOREIGN KEY (`group_challenge_participant_id`) REFERENCES `group_challenge_participants` (`group_challenge_participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `social_login_users` (
  `created_at` datetime(6) NOT NULL,
  `provider_refresh_token_updated_at` datetime(6) DEFAULT NULL,
  `social_login_user_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `provider_user_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_refresh_token` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider` enum('APPLE','KAKAO','TEST') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`social_login_user_id`),
  UNIQUE KEY `uk_social_login_users_provider_user` (`provider`,`provider_user_id`),
  UNIQUE KEY `uk_social_login_users_user_provider` (`user_id`,`provider`),
  CONSTRAINT `FKjl5ghtlonya2ta3ehq88mvcux` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `time_limits` (
  `total_lock_minutes` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `time_limit_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`time_limit_id`),
  UNIQUE KEY `UK7h2tk6t0uwa2fhlans2c5i69v` (`user_id`),
  CONSTRAINT `FKim23dnpbbfxv1y7r5dtv3vir7` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `time_limits_chk_1` CHECK ((`total_lock_minutes` between 0 and 1440))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `transfer_phrase` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_phrase` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `usage_goal_type` (
  `usage_goal_type_id` bigint NOT NULL,
  `description` enum('INSTAGRAM','TOTAL_USAGE','YOUTUBE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`usage_goal_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_usage_goal_times` (
  `goal_minutes` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `usage_goal_type_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `user_usage_goal_times_id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`user_usage_goal_times_id`),
  KEY `FKgwps29acwhh69m6sqnqhvxdiy` (`usage_goal_type_id`),
  KEY `FKkv4tmx5ict876r06pxaffw5kl` (`user_id`),
  CONSTRAINT `FKgwps29acwhh69m6sqnqhvxdiy` FOREIGN KEY (`usage_goal_type_id`) REFERENCES `usage_goal_type` (`usage_goal_type_id`),
  CONSTRAINT `FKkv4tmx5ict876r06pxaffw5kl` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
  `push_notification_enabled` bit(1) NOT NULL,
  `user_code` varchar(5) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `withdrawn_at` datetime(6) DEFAULT NULL,
  `display_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(320) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_image_object_key` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACTIVE','WITHDRAWN') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_users_user_code` (`user_code`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Constraints and stable index names from manual schema files that Hibernate
-- does not fully reproduce from the current entity annotations.
ALTER TABLE `first_screen_time`
  ADD KEY `idx_first_screen_time_user_id` (`user_id`);

ALTER TABLE `users`
  RENAME INDEX `UK6dotkott2kjsp8vw4d0m25fb7` TO `uk_users_email`;

ALTER TABLE `time_limits`
  RENAME INDEX `UK7h2tk6t0uwa2fhlans2c5i69v` TO `uk_time_limits_user_id`;

ALTER TABLE `friend_invites`
  MODIFY COLUMN `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  MODIFY COLUMN `code` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  RENAME INDEX `UKd4v65hc5sklemgchilekx093k` TO `uk_friend_invites_user`,
  RENAME INDEX `UKhiqgbgmrj22wuhfymhu09042b` TO `uk_friend_invites_code`,
  ADD CONSTRAINT `fk_friend_invites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `friends`
  MODIFY COLUMN `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  MODIFY COLUMN `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  MODIFY COLUMN `status` enum('ACCEPTED','PENDING') CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
  ADD UNIQUE KEY `uk_friends_pair` ((LEAST(`from_user_id`, `to_user_id`)), (GREATEST(`from_user_id`, `to_user_id`))),
  ADD KEY `idx_friends_from_status` (`from_user_id`, `status`),
  ADD KEY `idx_friends_to_status` (`to_user_id`, `status`),
  ADD CONSTRAINT `fk_friends_from` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `fk_friends_to` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `ck_friends_not_self` CHECK (`from_user_id` <> `to_user_id`),
  ADD CONSTRAINT `ck_friends_accepted_at` CHECK (
    (`status` = 'PENDING' AND `accepted_at` IS NULL)
    OR (`status` = 'ACCEPTED' AND `accepted_at` IS NOT NULL)
  );

SET FOREIGN_KEY_CHECKS = @detoxmate_old_fk_checks;

-- Data is separate: the application seeds notification_type/notification at
-- startup; usage_goal_type and transfer_phrase require their own seed SQL.
