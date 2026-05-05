> 작성자: Codex
> 작성일: 2026-04-19
> 상태: Draft
>

---

## 1. 문서 목적

이 문서는 DetoxMate의 그룹/챌린지 도메인 용어를 팀 내에서 일관되게 사용하기 위한 기준 문서이다.

- 그룹과 챌린지를 어떤 개념으로 나누는지 정리한다.
- 각 테이블의 역할과 관계를 설명한다.
- MVP에서 단순화할 정책과 이후 확장 가능한 지점을 구분한다.

---

## 2. 핵심 개념

### Group

친구들이 모이는 모임 자체이다.

- 그룹명과 초대코드를 가진다.
- 챌린지가 없어도 유지된다.
- 다음 시즌의 디톡스도 같은 그룹에서 다시 시작할 수 있다.

### GroupChallenge

그룹이 특정 기간 동안 진행하는 한 번의 디톡스이다.

- 그룹의 시즌 또는 회차 개념이다.
- 시작일과 종료일을 가진다.
- 같은 그룹은 여러 번의 챌린지를 가질 수 있다.

### GroupMember

유저와 그룹의 소속 관계이다.

- 어떤 유저가 어떤 그룹에 속해 있는지 나타낸다.
- 방장 여부는 이 테이블에서 관리한다.
- 그룹에 속해 있지만 이번 챌린지에는 참가하지 않을 수 있다.

### GroupChallengeParticipant

유저가 이번 챌린지에 실제로 참가했는지를 나타내는 테이블이다.

- 시즌별 참가 여부를 관리한다.
- 시즌별 앱 사용 목표 시간을 스냅샷으로 연결한다.
- 챌린지 중도 이탈도 이 테이블에서 관리한다.

---

## 3. 왜 Group 과 GroupChallenge 를 분리하는가

현재 서비스에서는 그룹과 챌린지가 비슷해 보일 수 있지만, 도메인상 완전히 같은 개념은 아니다.

- 그룹은 계속 유지된다.
- 챌린지는 기간 단위로 시작하고 종료된다.
- 그룹 멤버는 유지되지만, 챌린지 참가자는 시즌마다 달라질 수 있다.

즉, `Group = 사람들의 모임`, `GroupChallenge = 이번 디톡스`로 보는 것이 맞다.

이렇게 분리해야 다음 케이스를 자연스럽게 처리할 수 있다.

- 같은 그룹이 1기, 2기, 3기 챌린지를 이어서 진행하는 경우
- 그룹은 그대로 두고, 다음 챌린지 참가자를 다시 모집하는 경우
- 그룹에 새로 들어온 멤버가 현재 진행 중 챌린지에는 참가하지 못하고 다음 챌린지를 기다리는 경우

---

## 4. 왜 GroupMember 와 GroupChallengeParticipant 를 분리하는가

두 테이블은 관리하는 단위가 다르다.

- `group_members`: 이 유저가 이 그룹에 속해 있는가
- `group_challenge_participants`: 이 유저가 이번 챌린지에 참가했는가

이 둘을 분리하지 않으면 아래 케이스를 처리하기 어렵다.

### 그룹에는 들어왔지만, 이번 챌린지에는 참가하지 않는 경우

- 챌린지 시작 후 그룹에 들어온 멤버
- 다음 시즌부터만 참여하려는 멤버

이 사람은 그룹 멤버이지만 챌린지 참가자는 아니다.

### 시즌마다 앱별 목표값이 달라지는 경우

- 유저의 앱 사용 목표 시간은 시즌마다 바뀔 수 있다.
- 인스타그램과 유튜브 제한 시간이 서로 다를 수 있다.
- 이 값은 유저 전역 기본값이 아니라 참가 시점의 스냅샷이어야 한다.

그래서 참가자별 앱 사용 목표 시간 스냅샷은 `usage_goal_time`에 두고, `group_challenge_participant_id`로 연결한다.
참가자 row는 먼저 만들 수 있고, 앱별 목표 시간 row는 나중에 0개 이상 추가하는 구조가 자연스럽다.

### 그룹 탈퇴와 챌린지 이탈을 다르게 다루는 경우

- 그룹은 남아 있지만 이번 챌린지만 쉬는 경우
- 챌린지는 나가지만 개인 기록은 남겨야 하는 경우

즉, 그룹 소속과 시즌 참가를 분리해야 정책을 바꿀 수 있는 여지가 생긴다.

---

## 5. 방장은 어디에 두는가

방장은 `group_members.role = OWNER`로 관리한다.

그 이유는 방장이 그룹 레벨의 개념이기 때문이다.

- 초대코드는 그룹 기준이다.
- 챌린지가 없어도 그룹은 존재한다.
- 챌린지가 끝나도 그룹은 남는다.

만약 방장을 `group_challenge_participants`에 두면 아래 문제가 생긴다.

- 챌린지가 없는 기간에는 방장이 사라진다.
- 이번 챌린지에 참가하지 않으면 방장도 아닌 상태가 된다.
- 챌린지가 끝날 때마다 그룹 소유권 의미가 흔들린다.

따라서 현재 방장은 그룹의 관리자이며, 시즌 참가자 개념과는 분리한다.

---

## 6. 멤버 기록은 히스토리로 남기는가

`group_members`는 현재 멤버만 표현하는 테이블이 아니라, 그룹과 유저의 소속 관계를 표현하는 테이블이다.

즉, 한 번 멤버가 된 유저의 row는 남기고 `status`로 현재 상태를 판단한다.

- `ACTIVE`: 현재 그룹 멤버
- `LEFT`: 자발적으로 나간 멤버
- `REMOVED`: 강제 제거된 멤버

이렇게 두는 이유는 다음과 같다.

- 과거에 누가 속해 있었는지 알 수 있다.
- 과거 챌린지 참가 기록과 연결이 유지된다.
- 탈퇴/재가입 정책을 서비스 로직에서 다루기 쉽다.

MVP에서는 `(group_id, user_id)`를 유니크로 두고, 재가입 시 기존 row를 재활성화하는 방식으로 운영한다.

---

## 7. 최종 테이블 구성

### 7.1 users

서비스 사용자 메타데이터

| column | type | description |
| --- | --- | --- |
| `user_id` | `BIGINT` | PK |
| `display_name` | `VARCHAR(30)` | 닉네임 |
| `profile_image_object_key` | `VARCHAR(1024)` | 프로필 이미지 S3 object key |
| `created_at` | `DATETIME(6)` | 생성일시 |
| `updated_at` | `DATETIME(6)` | 수정일시 |

### 7.2 groups

그룹 자체

| column | type | description |
| --- | --- | --- |
| `group_id` | `BIGINT` | PK |
| `name` | `VARCHAR(12)` | 그룹명 |
| `invite_code` | `VARCHAR(10)` | 그룹 초대코드 |
| `created_at` | `DATETIME(6)` | 생성일시 |
| `updated_at` | `DATETIME(6)` | 수정일시 |

메모:

- 그룹 상태 컬럼은 두지 않는다.
- 그룹의 진행 상태는 현재 챌린지 상태로 판단한다.

### 7.3 group_members

유저의 그룹 소속

| column | type | description |
| --- | --- | --- |
| `group_member_id` | `BIGINT` | PK |
| `group_id` | `BIGINT` | FK -> groups |
| `user_id` | `BIGINT` | FK -> users |
| `role` | `VARCHAR(20)` | `OWNER`, `MEMBER` |
| `status` | `VARCHAR(20)` | `ACTIVE`, `LEFT`, `REMOVED` |
| `joined_at` | `DATETIME(6)` | 그룹 가입일시 |
| `left_at` | `DATETIME(6)` | 그룹 탈퇴일시 |
| `created_at` | `DATETIME(6)` | 생성일시 |
| `updated_at` | `DATETIME(6)` | 수정일시 |

### 7.4 group_challenges

그룹의 한 번의 디톡스 실행

| column | type | description |
| --- | --- | --- |
| `group_challenge_id` | `BIGINT` | PK |
| `group_id` | `BIGINT` | FK -> groups |
| `challenge_no` | `INT` | 그룹 내 챌린지 순번 |
| `status` | `VARCHAR(20)` | `RECRUITING`, `ACTIVE`, `COMPLETED`, `CANCELED` |
| `start_at` | `DATETIME(6)` | 시작일시 |
| `end_at` | `DATETIME(6)` | 종료일시 |
| `created_at` | `DATETIME(6)` | 생성일시 |
| `updated_at` | `DATETIME(6)` | 수정일시 |

메모:

- `challenge_no`는 1기, 2기 같은 회차 개념이다.
- 한 그룹에는 동시에 `ACTIVE` 챌린지가 1개만 존재할 수 있다.

### 7.5 group_challenge_participants

이번 챌린지의 실제 참가자

| column | type | description |
| --- | --- | --- |
| `group_challenge_participant_id` | `BIGINT` | PK |
| `group_challenge_id` | `BIGINT` | FK -> group_challenges |
| `group_member_id` | `BIGINT` | FK -> group_members |
| `status` | `VARCHAR(20)` | `JOINED`, `WITHDRAWN`, `COMPLETED` |
| `joined_at` | `DATETIME(6)` | 챌린지 참가일시 |
| `withdrawn_at` | `DATETIME(6)` | 챌린지 이탈일시 |
| `baseline_screen_time_minutes` | `INT` | 시작 시 기준 스크린타임 |
| `created_at` | `DATETIME(6)` | 생성일시 |
| `updated_at` | `DATETIME(6)` | 수정일시 |

메모:

- 앱 사용 목표 시간 스냅샷은 `usage_goal_time`에 분리한다.
- 중도 이탈 시 row를 삭제하지 않고 상태를 변경한다.

### 7.6 usage_goal_type

앱 사용 목표 시간의 타입 정의

| column | type | description |
| --- | --- | --- |
| `usage_goal_type_id` | `BIGINT` | PK |
| `description` | `VARCHAR(50)` | `INSTAGRAM`, `YOUTUBE`, `TOTAL_USAGE` |

메모:

- MVP에서 실제 노출 타입은 `INSTAGRAM`, `YOUTUBE`만 사용한다.
- `TOTAL_USAGE`는 전체 사용시간 기준 값이다.

### 7.7 usage_goal_time

참가자별 앱 사용 목표 시간 스냅샷

| column | type | description |
| --- | --- | --- |
| `usage_goal_time_id` | `VARCHAR(255)` | PK |
| `usage_goal_type_id` | `BIGINT` | FK -> usage_goal_type |
| `group_challenge_participant_id` | `BIGINT` | FK -> group_challenge_participants |
| `goal_minutes` | `INT` | 앱별 목표 시간(분) |

메모:

- 한 참가자는 0개 이상의 앱 사용 목표 시간 스냅샷을 가질 수 있다.
- 이 값은 유저 전역 기본값이 아니라 이번 챌린지에 적용되는 고정값이다.
- 참가자가 먼저 생성되고 앱별 목표 시간 row는 나중에 추가될 수 있다.
- 예: 인스타그램 30분, 유튜브 60분
- 같은 참가자에 같은 타입이 중복되지 않도록 `(group_challenge_participant_id, usage_goal_type_id)` 유니크 제약을 권장한다.
- 향후 유저 전역 기본 목표 시간이 필요해지면 `user_usage_goal_times`를 별도로 도입하고, 참가 시점에 이 테이블로 복사하는 구조를 고려한다.

---

## 8. 테이블 관계 요약

- 한 `User`는 여러 `Group`에 속할 수 있다.
- 한 `Group`은 여러 `GroupMember`를 가진다.
- 한 `Group`은 여러 `GroupChallenge`를 가진다.
- 한 `GroupChallenge`는 여러 `GroupChallengeParticipant`를 가진다.
- 한 `GroupMember`는 여러 챌린지에 참가할 수 있다.
- 한 `GroupChallengeParticipant`는 0개 이상의 `usage_goal_time` 스냅샷을 가진다.

---

## 9. 현재까지 합의한 주요 정책

### 그룹과 챌린지

- 그룹 초대코드는 그룹 기준이다.
- 그룹은 챌린지가 끝나도 유지된다.
- 그룹마다 여러 챌린지를 가질 수 있다.
- 한 그룹에는 동시에 `ACTIVE` 챌린지가 1개만 가능하다.

### 유저 참여

- 한 유저는 여러 그룹에 참여할 수 있다.
- 도메인상 여러 활성 챌린지 참여도 가능하다.
- 다만 MVP에서는 서비스 로직으로 활성 챌린지 1개만 참여 가능하게 제한한다.

### 참가 정책

- 첫 챌린지는 바로 참여할 수 있다.
- 이후 챌린지는 시즌마다 참가 선택이 필요하다.
- 챌린지가 시작된 후에는 신규 참가가 불가능하다.

### 방장 정책

- 방장은 `group_members.role = OWNER`로 관리한다.
- 방장이 탈퇴하면 남아 있는 `ACTIVE` 멤버 중 한 명에게 `OWNER`를 랜덤 위임하는 정책을 사용한다.
- 이번 턴에서는 그룹 탈퇴 API만 먼저 구현하고, 방장 권한 랜덤 위임은 후속 구현으로 남긴다.

### 이탈 정책

- MVP에서는 유저 액션을 단순화하기 위해 진행 중 나가기를 하나의 액션으로 제공할 수 있다.
- 이 경우 내부적으로는 `최신 챌린지 이탈 + 그룹 탈퇴`를 함께 처리한다.
- 과거 챌린지는 이미 정상 수행한 이력으로 보고 참가 상태를 변경하지 않는다.
- 현재는 누구나 그룹 탈퇴를 요청할 수 있다.

---

## 10. MVP 설계 원칙

MVP에서는 유저가 느끼는 정책은 단순하게 가져가되, 데이터 모델은 확장 가능하게 유지한다.

- 데이터 모델은 `그룹 소속`과 `챌린지 참가`를 분리한다.
- 하지만 초기 UX에서는 둘을 완전히 다른 액션으로 노출하지 않아도 된다.
- 즉, 스키마는 확장 가능하게 설계하고, 초기 서비스 로직은 단순하게 운영한다.

이 원칙을 따르면 현재 요구사항을 처리하면서도, 이후 "그룹에는 남고 이번 시즌만 쉬기" 같은 정책으로 확장할 수 있다.
