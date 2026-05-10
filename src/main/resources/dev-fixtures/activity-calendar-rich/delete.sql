DELETE FROM comments
WHERE challenge_record_id IN (
    SELECT challenge_record_id
    FROM challenge_record
    WHERE group_challenge_id IN (
        SELECT group_challenge_id
        FROM group_challenges
        WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
    )
);

DELETE FROM reactions
WHERE challenge_record_id IN (
    SELECT challenge_record_id
    FROM challenge_record
    WHERE group_challenge_id IN (
        SELECT group_challenge_id
        FROM group_challenges
        WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
    )
);

DELETE FROM pokes
WHERE challenge_record_id IN (
    SELECT challenge_record_id
    FROM challenge_record
    WHERE group_challenge_id IN (
        SELECT group_challenge_id
        FROM group_challenges
        WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
    )
);

DELETE FROM challenge_record_status
WHERE challenge_record_id IN (
    SELECT challenge_record_id
    FROM challenge_record
    WHERE group_challenge_id IN (
        SELECT group_challenge_id
        FROM group_challenges
        WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
    )
);

DELETE FROM activity_record_detail
WHERE activity_record_id IN (
    SELECT activity_record_id
    FROM activity_record
    WHERE group_challenge_participant_id IN (
        SELECT group_challenge_participant_id
        FROM group_challenge_participants
        WHERE group_challenge_id IN (
            SELECT group_challenge_id
            FROM group_challenges
            WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
        )
    )
);

DELETE FROM challenge_record
WHERE group_challenge_id IN (
    SELECT group_challenge_id
    FROM group_challenges
    WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
);

DELETE FROM activity_record
WHERE group_challenge_participant_id IN (
    SELECT group_challenge_participant_id
    FROM group_challenge_participants
    WHERE group_challenge_id IN (
        SELECT group_challenge_id
        FROM group_challenges
        WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
    )
);

DELETE FROM user_usage_goal_times
WHERE user_id IN (
    SELECT user_id
    FROM group_members
    WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
);

DELETE FROM refresh_token_session
WHERE user_id IN (
    SELECT user_id
    FROM group_members
    WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
);

DELETE FROM social_login_users
WHERE user_id IN (
    SELECT user_id
    FROM group_members
    WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
);

DELETE FROM fcm_token
WHERE user_id IN (
    SELECT user_id
    FROM group_members
    WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
);

DELETE FROM group_challenge_participants
WHERE group_challenge_id IN (
    SELECT group_challenge_id
    FROM group_challenges
    WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
);

DELETE FROM group_challenges
WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01');

DELETE FROM users
WHERE user_id IN (
    SELECT user_id
    FROM group_members
    WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01')
);

DELETE FROM group_members
WHERE group_id IN (SELECT group_id FROM __GROUPS_TABLE__ WHERE invite_code = 'ACR01');

DELETE FROM __GROUPS_TABLE__
WHERE invite_code = 'ACR01';
