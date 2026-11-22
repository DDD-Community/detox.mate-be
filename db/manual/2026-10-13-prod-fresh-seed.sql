-- One-time data seed for a fresh Detoxmate database after
-- 2026-10-13-prod-fresh-schema.sql. Do not rerun without checking existing rows.
-- The application creates notification_type and notification templates at startup.

SET NAMES utf8mb4;

INSERT INTO usage_goal_type (usage_goal_type_id, description)
VALUES
    (1, 'TOTAL_USAGE'),
    (2, 'INSTAGRAM'),
    (3, 'YOUTUBE');

-- The 15 phrases and author names were confirmed in the earlier product chat.
-- The API returns the stored string without trimming or formatting it.
INSERT INTO transfer_phrase (transfer_phrase)
VALUES
    ('우리가 반복해서 하는 행동이 곧 우리 자신이다. - 윌 듀랜트'),
    ('오늘 할 수 있는 일을 내일로 미루지 마라. - 벤저민 프랭클린'),
    ('적어도 시도해 보자. 노력하면 잃을 게 없다. - 손흥민'),
    ('내가 하는 모든 행동은 내가 되고 싶은 사람에게 던지는 한 표다 - 제임스 클리어'),
    ('시작하는 방법은 말을 멈추고 행동하는 것이다. - 월트 디즈니'),
    ('어려워서 감히 하지 않는 게 아니다. 감히 하지 않아서 어려운 것이다. - 세네카'),
    ('자기 자신을 다스리지 못하는 사람은 자유롭지 않다. - 에픽테토스'),
    ('훈련의 고통은 잠시지만, 후회의 고통은 오래간다. - 짐 론'),
    ('속도가 느려도 멈추지만 않으면 괜찮다. - 공자'),
    ('시간을 사랑하는가? 그렇다면 낭비하지 마라. 시간은 삶을 이루는 재료다. - 벤저민 프랭클린'),
    ('내가 주의를 기울이기로 한 것이 곧 나의 경험이 된다. - 윌리엄 제임스'),
    ('집중한다는 건 다른 좋은 것들에 ''아니오''라고 말하는 것이다. - 스티브 잡스'),
    ('혼자서는 할 수 있는 일이 적지만, 함께하면 많은 일을 할 수 있다. - 헬렌 켈러'),
    ('우리는 시간이 짧은 게 아니라, 시간을 많이 낭비하는 것이다. - 세네카'),
    ('당신의 시간은 한정되어 있다. 다른 사람의 삶을 사느라 낭비하지 마라. - 스티브 잡스');

SELECT COUNT(*) AS usage_goal_type_count FROM usage_goal_type;
SELECT COUNT(*) AS transfer_phrase_count FROM transfer_phrase;
