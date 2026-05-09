INSERT INTO usage_goal_type (usage_goal_type_id, description)
SELECT -910000001, 'TOTAL_USAGE'
WHERE NOT EXISTS (
    SELECT 1 FROM usage_goal_type WHERE description = 'TOTAL_USAGE'
);

INSERT INTO users (user_id, display_name, created_at, updated_at, profile_image_object_key) VALUES
(-910000001, '캘린더 나', '__ME_JOINED_AT__', '__ME_JOINED_AT__', 'profiles/fixtures/calendar-me.png'),
(-910000002, '캘린더 지수', '__JISOO_JOINED_AT__', '__JISOO_JOINED_AT__', 'profiles/fixtures/calendar-jisoo.png'),
(-910000003, '캘린더 민준', '__MINJUN_JOINED_AT__', '__MINJUN_JOINED_AT__', 'profiles/fixtures/calendar-minjun.png');

INSERT INTO __GROUPS_TABLE__ (group_id, name, invite_code, created_at, updated_at) VALUES
(-910000001, '캘린더시드', 'ACR01', '__ME_JOINED_AT__', '__ME_JOINED_AT__');

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
(-910000001, -910000001, 1, 'ACTIVE', '__FIRST_VERIFICATION_AT__', NULL, '__ME_JOINED_AT__', '__FIRST_VERIFICATION_AT__');

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
(-910000001, -910000001, -910000001, 'OWNER', 'ACTIVE', '__ME_JOINED_AT__', NULL, '__ME_JOINED_AT__', '__ME_JOINED_AT__'),
(-910000002, -910000001, -910000002, 'MEMBER', 'ACTIVE', '__JISOO_JOINED_AT__', NULL, '__JISOO_JOINED_AT__', '__JISOO_JOINED_AT__'),
(-910000003, -910000001, -910000003, 'MEMBER', 'ACTIVE', '__MINJUN_JOINED_AT__', NULL, '__MINJUN_JOINED_AT__', '__MINJUN_JOINED_AT__');

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
(-910000001, -910000001, -910000001, 'JOINED', '__ME_JOINED_AT__', NULL, NULL, '__ME_JOINED_AT__', '__ME_JOINED_AT__'),
(-910000002, -910000001, -910000002, 'JOINED', '__JISOO_JOINED_AT__', NULL, NULL, '__JISOO_JOINED_AT__', '__JISOO_JOINED_AT__'),
(-910000003, -910000001, -910000003, 'JOINED', '__MINJUN_JOINED_AT__', NULL, NULL, '__MINJUN_JOINED_AT__', '__MINJUN_JOINED_AT__');

INSERT INTO user_usage_goal_times (
    user_usage_goal_times_id,
    usage_goal_type_id,
    goal_minutes,
    created_at,
    user_id,
    updated_at
) VALUES
(-910000001, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), 120, '__ME_GOAL_SET_AT__', -910000001, '__ME_GOAL_SET_AT__'),
(-910000002, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), 120, '__JISOO_GOAL_SET_AT__', -910000002, '__JISOO_GOAL_SET_AT__'),
(-910000003, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), 120, '__MINJUN_GOAL_SET_AT__', -910000003, '__MINJUN_GOAL_SET_AT__');

INSERT INTO activity_record (
    activity_record_id,
    user_id,
    group_challenge_participant_id,
    activity_image_object_key,
    reflection_text,
    created_at
) VALUES
(-910001001, -910000001, -910000001, 'activity-records/fixtures/calendar-me/__D_MINUS_8__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_8_ME_AT__'),
(-910001002, -910000001, -910000001, 'activity-records/fixtures/calendar-me/__D_MINUS_7__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_7_ME_AT__'),
(-910001003, -910000001, -910000001, 'activity-records/fixtures/calendar-me/__D_MINUS_6__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_6_ME_AT__'),
(-910001004, -910000002, -910000002, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_6__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_6_JISOO_AT__'),
(-910001005, -910000001, -910000001, 'activity-records/fixtures/calendar-me/__D_MINUS_5__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_5_ME_AT__'),
(-910001006, -910000001, -910000001, 'activity-records/fixtures/calendar-me/__D_MINUS_4__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_4_ME_AT__'),
(-910001007, -910000002, -910000002, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_4__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_4_JISOO_AT__'),
(-910001008, -910000003, -910000003, 'activity-records/fixtures/calendar-minjun/__D_MINUS_4__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_4_MINJUN_AT__'),
(-910001009, -910000002, -910000002, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_3__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_3_JISOO_AT__'),
(-910001010, -910000003, -910000003, 'activity-records/fixtures/calendar-minjun/__D_MINUS_3__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_3_MINJUN_AT__'),
(-910001011, -910000001, -910000001, 'activity-records/fixtures/calendar-me/__D_MINUS_2__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_2_ME_AT__'),
(-910001012, -910000002, -910000002, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_2__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_2_JISOO_AT__'),
(-910001013, -910000003, -910000003, 'activity-records/fixtures/calendar-minjun/__D_MINUS_2__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_2_MINJUN_AT__'),
(-910001014, -910000001, -910000001, 'activity-records/fixtures/calendar-me/__D_MINUS_1__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_1_ME_AT__'),
(-910001015, -910000002, -910000002, 'activity-records/fixtures/calendar-jisoo/__D_MINUS_1__.png', '오늘도 목표 시간 안에서 잘 버텼어요.', '__D_MINUS_1_JISOO_AT__');

INSERT INTO activity_record_detail (
    activity_record_detail_id,
    use_minutes,
    activity_record_id,
    usage_goal_type_id,
    user_usage_goal_times_id,
    is_achieved,
    created_at
) VALUES
(-910003001, 90, -910001001, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000001, true, '__D_MINUS_8_ME_AT__'),
(-910003002, 90, -910001002, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000001, true, '__D_MINUS_7_ME_AT__'),
(-910003003, 90, -910001003, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000001, true, '__D_MINUS_6_ME_AT__'),
(-910003004, 80, -910001004, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000002, true, '__D_MINUS_6_JISOO_AT__'),
(-910003005, 90, -910001005, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000001, true, '__D_MINUS_5_ME_AT__'),
(-910003006, 90, -910001006, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000001, true, '__D_MINUS_4_ME_AT__'),
(-910003007, 80, -910001007, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000002, true, '__D_MINUS_4_JISOO_AT__'),
(-910003008, 100, -910001008, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000003, true, '__D_MINUS_4_MINJUN_AT__'),
(-910003009, 80, -910001009, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000002, true, '__D_MINUS_3_JISOO_AT__'),
(-910003010, 100, -910001010, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000003, true, '__D_MINUS_3_MINJUN_AT__'),
(-910003011, 90, -910001011, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000001, true, '__D_MINUS_2_ME_AT__'),
(-910003012, 80, -910001012, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000002, true, '__D_MINUS_2_JISOO_AT__'),
(-910003013, 100, -910001013, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000003, true, '__D_MINUS_2_MINJUN_AT__'),
(-910003014, 90, -910001014, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000001, true, '__D_MINUS_1_ME_AT__'),
(-910003015, 80, -910001015, (SELECT MIN(usage_goal_type_id) FROM usage_goal_type WHERE description = 'TOTAL_USAGE'), -910000002, true, '__D_MINUS_1_JISOO_AT__');

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
(-910002001, -910000001, -910000001, '__D_MINUS_8__', -910001001, 'AFTER_RECORD_SUCCESS', '__D_MINUS_8_ME_AT__', '__D_MINUS_8_ME_AT__'),
(-910002002, -910000001, -910000001, '__D_MINUS_7__', -910001002, 'AFTER_RECORD_SUCCESS', '__D_MINUS_7_ME_AT__', '__D_MINUS_7_ME_AT__'),
(-910002003, -910000001, -910000001, '__D_MINUS_6__', -910001003, 'AFTER_RECORD_SUCCESS', '__D_MINUS_6_ME_AT__', '__D_MINUS_6_ME_AT__'),
(-910002004, -910000001, -910000002, '__D_MINUS_6__', -910001004, 'AFTER_RECORD_SUCCESS', '__D_MINUS_6_JISOO_AT__', '__D_MINUS_6_JISOO_AT__'),
(-910002005, -910000001, -910000001, '__D_MINUS_5__', -910001005, 'AFTER_RECORD_SUCCESS', '__D_MINUS_5_ME_AT__', '__D_MINUS_5_ME_AT__'),
(-910002006, -910000001, -910000001, '__D_MINUS_4__', -910001006, 'AFTER_RECORD_SUCCESS', '__D_MINUS_4_ME_AT__', '__D_MINUS_4_ME_AT__'),
(-910002007, -910000001, -910000002, '__D_MINUS_4__', -910001007, 'AFTER_RECORD_SUCCESS', '__D_MINUS_4_JISOO_AT__', '__D_MINUS_4_JISOO_AT__'),
(-910002008, -910000001, -910000003, '__D_MINUS_4__', -910001008, 'AFTER_RECORD_SUCCESS', '__D_MINUS_4_MINJUN_AT__', '__D_MINUS_4_MINJUN_AT__'),
(-910002009, -910000001, -910000002, '__D_MINUS_3__', -910001009, 'AFTER_RECORD_SUCCESS', '__D_MINUS_3_JISOO_AT__', '__D_MINUS_3_JISOO_AT__'),
(-910002010, -910000001, -910000003, '__D_MINUS_3__', -910001010, 'AFTER_RECORD_SUCCESS', '__D_MINUS_3_MINJUN_AT__', '__D_MINUS_3_MINJUN_AT__'),
(-910002011, -910000001, -910000001, '__D_MINUS_2__', -910001011, 'AFTER_RECORD_SUCCESS', '__D_MINUS_2_ME_AT__', '__D_MINUS_2_ME_AT__'),
(-910002012, -910000001, -910000002, '__D_MINUS_2__', -910001012, 'AFTER_RECORD_SUCCESS', '__D_MINUS_2_JISOO_AT__', '__D_MINUS_2_JISOO_AT__'),
(-910002013, -910000001, -910000003, '__D_MINUS_2__', -910001013, 'AFTER_RECORD_SUCCESS', '__D_MINUS_2_MINJUN_AT__', '__D_MINUS_2_MINJUN_AT__'),
(-910002014, -910000001, -910000001, '__D_MINUS_1__', -910001014, 'AFTER_RECORD_SUCCESS', '__D_MINUS_1_ME_AT__', '__D_MINUS_1_ME_AT__'),
(-910002015, -910000001, -910000002, '__D_MINUS_1__', -910001015, 'AFTER_RECORD_SUCCESS', '__D_MINUS_1_JISOO_AT__', '__D_MINUS_1_JISOO_AT__');

INSERT INTO challenge_record_status (
    challenge_record_status_id,
    challenge_record_id,
    before_comment_count,
    after_comment_count,
    reaction_count,
    poke_count,
    created_at
) VALUES
(-910004001, -910002001, 0, 0, 0, 0, '__D_MINUS_8_ME_AT__'),
(-910004002, -910002002, 0, 0, 0, 0, '__D_MINUS_7_ME_AT__'),
(-910004003, -910002003, 0, 0, 0, 0, '__D_MINUS_6_ME_AT__'),
(-910004004, -910002004, 0, 0, 0, 0, '__D_MINUS_6_JISOO_AT__'),
(-910004005, -910002005, 0, 0, 0, 0, '__D_MINUS_5_ME_AT__'),
(-910004006, -910002006, 0, 0, 0, 0, '__D_MINUS_4_ME_AT__'),
(-910004007, -910002007, 0, 0, 0, 0, '__D_MINUS_4_JISOO_AT__'),
(-910004008, -910002008, 0, 0, 0, 0, '__D_MINUS_4_MINJUN_AT__'),
(-910004009, -910002009, 0, 0, 0, 0, '__D_MINUS_3_JISOO_AT__'),
(-910004010, -910002010, 0, 0, 0, 0, '__D_MINUS_3_MINJUN_AT__'),
(-910004011, -910002011, 0, 0, 0, 0, '__D_MINUS_2_ME_AT__'),
(-910004012, -910002012, 0, 0, 0, 0, '__D_MINUS_2_JISOO_AT__'),
(-910004013, -910002013, 0, 0, 0, 0, '__D_MINUS_2_MINJUN_AT__'),
(-910004014, -910002014, 0, 0, 0, 0, '__D_MINUS_1_ME_AT__'),
(-910004015, -910002015, 0, 0, 0, 0, '__D_MINUS_1_JISOO_AT__');
