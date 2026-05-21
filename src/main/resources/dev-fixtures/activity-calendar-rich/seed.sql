INSERT INTO usage_goal_type (usage_goal_type_id, description)
SELECT __TOTAL_USAGE_GOAL_TYPE_ID__, 'TOTAL_USAGE'
WHERE NOT EXISTS (
    SELECT 1 FROM usage_goal_type WHERE description = 'TOTAL_USAGE'
);

INSERT INTO users (user_id, display_name, created_at, updated_at, profile_image_object_key, push_notification_enabled) VALUES
(__ME_USER_ID__, '캘린더 나', '__ME_JOINED_AT__', '__ME_JOINED_AT__', 'profiles/fixtures/calendar-me.png', true),
(__JISOO_USER_ID__, '캘린더 지수', '__JISOO_JOINED_AT__', '__JISOO_JOINED_AT__', 'profiles/fixtures/calendar-jisoo.png', true),
(__MINJUN_USER_ID__, '캘린더 민준', '__MINJUN_JOINED_AT__', '__MINJUN_JOINED_AT__', 'profiles/fixtures/calendar-minjun.png', true);

INSERT INTO __GROUPS_TABLE__ (group_id, name, invite_code, created_at, updated_at) VALUES
(__GROUP_ID__, '캘린더시드', 'ACR01', '__ME_JOINED_AT__', '__ME_JOINED_AT__');

INSERT INTO group_challenges (
    group_challenge_id,
    group_id,
    challenge_no,
    status,
    start_at,
    end_at,
    created_at,
    updated_at
) VALUES
(__GROUP_CHALLENGE_ID__, __GROUP_ID__, 1, 'ACTIVE', '__CHALLENGE_START_AT__', NULL, '__ME_JOINED_AT__', '__CHALLENGE_START_AT__');

INSERT INTO group_members (
    group_member_id,
    group_id,
    user_id,
    role,
    status,
    joined_at,
    left_at,
    created_at,
    updated_at
) VALUES
(__ME_GROUP_MEMBER_ID__, __GROUP_ID__, __ME_USER_ID__, 'OWNER', 'ACTIVE', '__ME_JOINED_AT__', NULL, '__ME_JOINED_AT__', '__ME_JOINED_AT__'),
(__JISOO_GROUP_MEMBER_ID__, __GROUP_ID__, __JISOO_USER_ID__, 'MEMBER', 'ACTIVE', '__JISOO_JOINED_AT__', NULL, '__JISOO_JOINED_AT__', '__JISOO_JOINED_AT__'),
(__MINJUN_GROUP_MEMBER_ID__, __GROUP_ID__, __MINJUN_USER_ID__, 'MEMBER', 'ACTIVE', '__MINJUN_JOINED_AT__', NULL, '__MINJUN_JOINED_AT__', '__MINJUN_JOINED_AT__');

INSERT INTO group_challenge_participants (
    group_challenge_participant_id,
    group_challenge_id,
    group_member_id,
    status,
    joined_at,
    withdrawn_at,
    baseline_screen_time_minutes,
    created_at,
    updated_at
) VALUES
(__ME_PARTICIPANT_ID__, __GROUP_CHALLENGE_ID__, __ME_GROUP_MEMBER_ID__, 'JOINED', '__ME_JOINED_AT__', NULL, NULL, '__ME_JOINED_AT__', '__ME_JOINED_AT__'),
(__JISOO_PARTICIPANT_ID__, __GROUP_CHALLENGE_ID__, __JISOO_GROUP_MEMBER_ID__, 'JOINED', '__JISOO_JOINED_AT__', NULL, NULL, '__JISOO_JOINED_AT__', '__JISOO_JOINED_AT__'),
(__MINJUN_PARTICIPANT_ID__, __GROUP_CHALLENGE_ID__, __MINJUN_GROUP_MEMBER_ID__, 'JOINED', '__MINJUN_JOINED_AT__', NULL, NULL, '__MINJUN_JOINED_AT__', '__MINJUN_JOINED_AT__');

INSERT INTO user_usage_goal_times (
    user_usage_goal_times_id,
    usage_goal_type_id,
    goal_minutes,
    created_at,
    user_id,
    updated_at
) VALUES
(__ME_GOAL_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), 120, '__ME_GOAL_SET_AT__', __ME_USER_ID__, '__ME_GOAL_SET_AT__'),
(__JISOO_GOAL_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), 120, '__JISOO_GOAL_SET_AT__', __JISOO_USER_ID__, '__JISOO_GOAL_SET_AT__'),
(__MINJUN_GOAL_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), 120, '__MINJUN_GOAL_SET_AT__', __MINJUN_USER_ID__, '__MINJUN_GOAL_SET_AT__');

INSERT INTO activity_record (
    activity_record_id,
    user_id,
    group_challenge_participant_id,
    activity_image_object_key,
    reflection_text,
    created_at
) VALUES
(__ACTIVITY_RECORD_1_ID__, __ME_USER_ID__, __ME_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-me/__D_MINUS_8__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_8_ME_AT__'),
(__ACTIVITY_RECORD_2_ID__, __ME_USER_ID__, __ME_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-me/__D_MINUS_7__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_7_ME_AT__'),
(__ACTIVITY_RECORD_3_ID__, __ME_USER_ID__, __ME_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-me/__D_MINUS_6__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_6_ME_AT__'),
(__ACTIVITY_RECORD_4_ID__, __JISOO_USER_ID__, __JISOO_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_6__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_6_JISOO_AT__'),
(__ACTIVITY_RECORD_5_ID__, __ME_USER_ID__, __ME_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-me/__D_MINUS_5__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_5_ME_AT__'),
(__ACTIVITY_RECORD_6_ID__, __ME_USER_ID__, __ME_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-me/__D_MINUS_4__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_4_ME_AT__'),
(__ACTIVITY_RECORD_7_ID__, __JISOO_USER_ID__, __JISOO_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_4__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_4_JISOO_AT__'),
(__ACTIVITY_RECORD_8_ID__, __MINJUN_USER_ID__, __MINJUN_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-minjun/__D_MINUS_4__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_4_MINJUN_AT__'),
(__ACTIVITY_RECORD_9_ID__, __JISOO_USER_ID__, __JISOO_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_3__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_3_JISOO_AT__'),
(__ACTIVITY_RECORD_10_ID__, __MINJUN_USER_ID__, __MINJUN_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-minjun/__D_MINUS_3__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_3_MINJUN_AT__'),
(__ACTIVITY_RECORD_11_ID__, __ME_USER_ID__, __ME_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-me/__D_MINUS_2__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_2_ME_AT__'),
(__ACTIVITY_RECORD_12_ID__, __JISOO_USER_ID__, __JISOO_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_2__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_2_JISOO_AT__'),
(__ACTIVITY_RECORD_13_ID__, __MINJUN_USER_ID__, __MINJUN_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-minjun/__D_MINUS_2__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_2_MINJUN_AT__'),
(__ACTIVITY_RECORD_14_ID__, __ME_USER_ID__, __ME_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-me/__D_MINUS_1__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_1_ME_AT__'),
(__ACTIVITY_RECORD_15_ID__, __JISOO_USER_ID__, __JISOO_PARTICIPANT_ID__, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_1__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_1_JISOO_AT__');

INSERT INTO activity_record_detail (
    activity_record_detail_id,
    use_minutes,
    activity_record_id,
    usage_goal_type_id,
    user_usage_goal_times_id,
    is_achieved,
    created_at
) VALUES
(__ACTIVITY_RECORD_DETAIL_1_ID__, 90, __ACTIVITY_RECORD_1_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __ME_GOAL_ID__, true, '__D_MINUS_8_ME_AT__'),
(__ACTIVITY_RECORD_DETAIL_2_ID__, 90, __ACTIVITY_RECORD_2_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __ME_GOAL_ID__, true, '__D_MINUS_7_ME_AT__'),
(__ACTIVITY_RECORD_DETAIL_3_ID__, 90, __ACTIVITY_RECORD_3_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __ME_GOAL_ID__, true, '__D_MINUS_6_ME_AT__'),
(__ACTIVITY_RECORD_DETAIL_4_ID__, 80, __ACTIVITY_RECORD_4_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __JISOO_GOAL_ID__, true, '__D_MINUS_6_JISOO_AT__'),
(__ACTIVITY_RECORD_DETAIL_5_ID__, 90, __ACTIVITY_RECORD_5_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __ME_GOAL_ID__, true, '__D_MINUS_5_ME_AT__'),
(__ACTIVITY_RECORD_DETAIL_6_ID__, 90, __ACTIVITY_RECORD_6_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __ME_GOAL_ID__, true, '__D_MINUS_4_ME_AT__'),
(__ACTIVITY_RECORD_DETAIL_7_ID__, 80, __ACTIVITY_RECORD_7_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __JISOO_GOAL_ID__, true, '__D_MINUS_4_JISOO_AT__'),
(__ACTIVITY_RECORD_DETAIL_8_ID__, 100, __ACTIVITY_RECORD_8_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __MINJUN_GOAL_ID__, true, '__D_MINUS_4_MINJUN_AT__'),
(__ACTIVITY_RECORD_DETAIL_9_ID__, 80, __ACTIVITY_RECORD_9_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __JISOO_GOAL_ID__, true, '__D_MINUS_3_JISOO_AT__'),
(__ACTIVITY_RECORD_DETAIL_10_ID__, 100, __ACTIVITY_RECORD_10_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __MINJUN_GOAL_ID__, true, '__D_MINUS_3_MINJUN_AT__'),
(__ACTIVITY_RECORD_DETAIL_11_ID__, 90, __ACTIVITY_RECORD_11_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __ME_GOAL_ID__, true, '__D_MINUS_2_ME_AT__'),
(__ACTIVITY_RECORD_DETAIL_12_ID__, 80, __ACTIVITY_RECORD_12_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __JISOO_GOAL_ID__, true, '__D_MINUS_2_JISOO_AT__'),
(__ACTIVITY_RECORD_DETAIL_13_ID__, 100, __ACTIVITY_RECORD_13_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __MINJUN_GOAL_ID__, true, '__D_MINUS_2_MINJUN_AT__'),
(__ACTIVITY_RECORD_DETAIL_14_ID__, 90, __ACTIVITY_RECORD_14_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __ME_GOAL_ID__, true, '__D_MINUS_1_ME_AT__'),
(__ACTIVITY_RECORD_DETAIL_15_ID__, 80, __ACTIVITY_RECORD_15_ID__, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), __JISOO_GOAL_ID__, true, '__D_MINUS_1_JISOO_AT__');

INSERT INTO challenge_record (
    challenge_record_id,
    group_challenge_id,
    group_challenge_participant_id,
    record_date,
    activity_record_id,
    status,
    created_at,
    updated_at
) VALUES
(__CHALLENGE_RECORD_1_ID__, __GROUP_CHALLENGE_ID__, __ME_PARTICIPANT_ID__, '__D_MINUS_8__', __ACTIVITY_RECORD_1_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_8_ME_AT__', '__D_MINUS_8_ME_AT__'),
(__CHALLENGE_RECORD_2_ID__, __GROUP_CHALLENGE_ID__, __ME_PARTICIPANT_ID__, '__D_MINUS_7__', __ACTIVITY_RECORD_2_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_7_ME_AT__', '__D_MINUS_7_ME_AT__'),
(__CHALLENGE_RECORD_3_ID__, __GROUP_CHALLENGE_ID__, __ME_PARTICIPANT_ID__, '__D_MINUS_6__', __ACTIVITY_RECORD_3_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_6_ME_AT__', '__D_MINUS_6_ME_AT__'),
(__CHALLENGE_RECORD_4_ID__, __GROUP_CHALLENGE_ID__, __JISOO_PARTICIPANT_ID__, '__D_MINUS_6__', __ACTIVITY_RECORD_4_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_6_JISOO_AT__', '__D_MINUS_6_JISOO_AT__'),
(__CHALLENGE_RECORD_5_ID__, __GROUP_CHALLENGE_ID__, __ME_PARTICIPANT_ID__, '__D_MINUS_5__', __ACTIVITY_RECORD_5_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_5_ME_AT__', '__D_MINUS_5_ME_AT__'),
(__CHALLENGE_RECORD_6_ID__, __GROUP_CHALLENGE_ID__, __ME_PARTICIPANT_ID__, '__D_MINUS_4__', __ACTIVITY_RECORD_6_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_4_ME_AT__', '__D_MINUS_4_ME_AT__'),
(__CHALLENGE_RECORD_7_ID__, __GROUP_CHALLENGE_ID__, __JISOO_PARTICIPANT_ID__, '__D_MINUS_4__', __ACTIVITY_RECORD_7_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_4_JISOO_AT__', '__D_MINUS_4_JISOO_AT__'),
(__CHALLENGE_RECORD_8_ID__, __GROUP_CHALLENGE_ID__, __MINJUN_PARTICIPANT_ID__, '__D_MINUS_4__', __ACTIVITY_RECORD_8_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_4_MINJUN_AT__', '__D_MINUS_4_MINJUN_AT__'),
(__CHALLENGE_RECORD_9_ID__, __GROUP_CHALLENGE_ID__, __JISOO_PARTICIPANT_ID__, '__D_MINUS_3__', __ACTIVITY_RECORD_9_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_3_JISOO_AT__', '__D_MINUS_3_JISOO_AT__'),
(__CHALLENGE_RECORD_10_ID__, __GROUP_CHALLENGE_ID__, __MINJUN_PARTICIPANT_ID__, '__D_MINUS_3__', __ACTIVITY_RECORD_10_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_3_MINJUN_AT__', '__D_MINUS_3_MINJUN_AT__'),
(__CHALLENGE_RECORD_11_ID__, __GROUP_CHALLENGE_ID__, __ME_PARTICIPANT_ID__, '__D_MINUS_2__', __ACTIVITY_RECORD_11_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_2_ME_AT__', '__D_MINUS_2_ME_AT__'),
(__CHALLENGE_RECORD_12_ID__, __GROUP_CHALLENGE_ID__, __JISOO_PARTICIPANT_ID__, '__D_MINUS_2__', __ACTIVITY_RECORD_12_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_2_JISOO_AT__', '__D_MINUS_2_JISOO_AT__'),
(__CHALLENGE_RECORD_13_ID__, __GROUP_CHALLENGE_ID__, __MINJUN_PARTICIPANT_ID__, '__D_MINUS_2__', __ACTIVITY_RECORD_13_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_2_MINJUN_AT__', '__D_MINUS_2_MINJUN_AT__'),
(__CHALLENGE_RECORD_14_ID__, __GROUP_CHALLENGE_ID__, __ME_PARTICIPANT_ID__, '__D_MINUS_1__', __ACTIVITY_RECORD_14_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_1_ME_AT__', '__D_MINUS_1_ME_AT__'),
(__CHALLENGE_RECORD_15_ID__, __GROUP_CHALLENGE_ID__, __JISOO_PARTICIPANT_ID__, '__D_MINUS_1__', __ACTIVITY_RECORD_15_ID__, 'AFTER_RECORD_SUCCESS', '__D_MINUS_1_JISOO_AT__', '__D_MINUS_1_JISOO_AT__');

INSERT INTO challenge_record_status (
    challenge_record_status_id,
    challenge_record_id,
    before_comment_count,
    after_comment_count,
    reaction_count,
    poke_count,
    created_at
) VALUES
(__CHALLENGE_RECORD_STATUS_1_ID__, __CHALLENGE_RECORD_1_ID__, 0, 0, 0, 0, '__D_MINUS_8_ME_AT__'),
(__CHALLENGE_RECORD_STATUS_2_ID__, __CHALLENGE_RECORD_2_ID__, 0, 0, 0, 0, '__D_MINUS_7_ME_AT__'),
(__CHALLENGE_RECORD_STATUS_3_ID__, __CHALLENGE_RECORD_3_ID__, 0, 0, 0, 0, '__D_MINUS_6_ME_AT__'),
(__CHALLENGE_RECORD_STATUS_4_ID__, __CHALLENGE_RECORD_4_ID__, 0, 0, 0, 0, '__D_MINUS_6_JISOO_AT__'),
(__CHALLENGE_RECORD_STATUS_5_ID__, __CHALLENGE_RECORD_5_ID__, 0, 0, 0, 0, '__D_MINUS_5_ME_AT__'),
(__CHALLENGE_RECORD_STATUS_6_ID__, __CHALLENGE_RECORD_6_ID__, 0, 0, 0, 0, '__D_MINUS_4_ME_AT__'),
(__CHALLENGE_RECORD_STATUS_7_ID__, __CHALLENGE_RECORD_7_ID__, 0, 0, 0, 0, '__D_MINUS_4_JISOO_AT__'),
(__CHALLENGE_RECORD_STATUS_8_ID__, __CHALLENGE_RECORD_8_ID__, 0, 0, 0, 0, '__D_MINUS_4_MINJUN_AT__'),
(__CHALLENGE_RECORD_STATUS_9_ID__, __CHALLENGE_RECORD_9_ID__, 0, 0, 0, 0, '__D_MINUS_3_JISOO_AT__'),
(__CHALLENGE_RECORD_STATUS_10_ID__, __CHALLENGE_RECORD_10_ID__, 0, 0, 0, 0, '__D_MINUS_3_MINJUN_AT__'),
(__CHALLENGE_RECORD_STATUS_11_ID__, __CHALLENGE_RECORD_11_ID__, 0, 0, 0, 0, '__D_MINUS_2_ME_AT__'),
(__CHALLENGE_RECORD_STATUS_12_ID__, __CHALLENGE_RECORD_12_ID__, 0, 0, 0, 0, '__D_MINUS_2_JISOO_AT__'),
(__CHALLENGE_RECORD_STATUS_13_ID__, __CHALLENGE_RECORD_13_ID__, 0, 0, 0, 0, '__D_MINUS_2_MINJUN_AT__'),
(__CHALLENGE_RECORD_STATUS_14_ID__, __CHALLENGE_RECORD_14_ID__, 0, 0, 0, 0, '__D_MINUS_1_ME_AT__'),
(__CHALLENGE_RECORD_STATUS_15_ID__, __CHALLENGE_RECORD_15_ID__, 0, 0, 0, 0, '__D_MINUS_1_JISOO_AT__');
