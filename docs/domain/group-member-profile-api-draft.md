> 작성자: Codex
> 작성일: 2026-05-05
> 상태: Draft
>

---

## 그룹 멤버 프로필 / 목표 조회 API 초안

### 원칙

- `/me/mypage` 화면 전용 API는 만들지 않는다.
- 마이페이지와 친구 프로필 화면은 목적별 API를 조합해서 사용한다.
- 그룹 멤버 프로필 조회는 그룹 하위 멤버 리소스로 표현한다.
- 목표 변경 가능 여부는 그룹 멤버 프로필이 아니라 내 목표 조회 API에서 제공한다.
- 프로필 이미지 변경은 이미지 URL이 아니라 presigned URL 발급 후 받은 `profileImageObjectKey`를 전달한다.
- URI의 path variable은 의미가 드러나도록 `{groupId}`, `{memberId}`로 쓴다.
- 날짜/시간은 ISO-8601 형식으로 내려준다.

### 상태값

- `role`: `OWNER`, `MEMBER`
- `usageGoalType`: `TOTAL_USAGE`, `INSTAGRAM`, `YOUTUBE`

---

## 공통 스키마

### UsageGoalResponse

현재 사용자 또는 그룹 멤버의 최신 목표 시간.

```json
{
  "id": 101,
  "usageGoalType": "TOTAL_USAGE",
  "goalMinutes": 60,
  "setAt": "2026-04-29T10:30:00"
}
```

필드 설명:

| 필드 | 설명 |
| --- | --- |
| `id` | `user_usage_goal_times` ID |
| `usageGoalType` | 목표 타입 |
| `goalMinutes` | 목표 시간(분) |
| `setAt` | 목표 설정 일시 |

### GoalChangeAvailabilityResponse

현재 로그인한 사용자의 목표 변경 가능 상태.

```json
{
  "canChange": false,
  "nextChangeAvailableAt": "2026-05-06T10:30:00",
  "remainingDays": 1
}
```

필드 설명:

| 필드 | 설명 |
| --- | --- |
| `canChange` | 현재 시점에 목표 변경 가능한지 여부 |
| `nextChangeAvailableAt` | 다음 목표 변경 가능 일시. 지금 변경 가능하면 `null` |
| `remainingDays` | 다음 변경 가능일까지 남은 일수. 지금 변경 가능하면 `0` |

### MemberStatsResponse

그룹 멤버의 목표 달성 통계.

```json
{
  "overall": {
    "startDate": "2026-04-29",
    "endDate": "2026-05-05",
    "totalDays": 7,
    "achievedDays": 3,
    "achievementRate": 43
  },
  "recent7Days": {
    "startDate": "2026-04-29",
    "endDate": "2026-05-05",
    "totalDays": 7,
    "submittedDays": 5,
    "achievedDays": 3
  }
}
```

필드 설명:

| 필드 | 설명 |
| --- | --- |
| `overall.startDate` | 전체 달성률 계산 시작일. 기본값은 최신 `TOTAL_USAGE` 목표 설정일 |
| `overall.endDate` | 전체 달성률 계산 종료일. 조회 당일 |
| `overall.totalDays` | 전체 달성률 분모 |
| `overall.achievedDays` | 전체 달성률 기간 중 달성일 수 |
| `overall.achievementRate` | `achievedDays / totalDays * 100`을 정수 반올림한 값 |
| `recent7Days.startDate` | 최근 7일 시작일 |
| `recent7Days.endDate` | 최근 7일 종료일. 조회 당일 |
| `recent7Days.totalDays` | 최근 7일 분모. 항상 `7` |
| `recent7Days.submittedDays` | 최근 7일 중 최종 인증 완료일 수 |
| `recent7Days.achievedDays` | 최근 7일 중 목표 달성일 수 |

---

## 1. GET /groups/{groupId}/members/{memberId}

그룹 멤버 프로필과 통계를 조회한다.

### Path Variables

| 이름 | 설명 |
| --- | --- |
| `groupId` | 그룹 ID |
| `memberId` | 그룹 멤버 ID |

### 동작

- 로그인 사용자가 `groupId` 그룹의 활성 멤버인지 확인한다.
- 대상 `memberId`가 `groupId` 그룹의 멤버 이력에 있는지 확인한다.
- 대상 멤버가 회원 탈퇴한 사용자이면 공개 프로필을 익명화하고 `isUserWithdrawn=true`를 반환한다.
- 대상 멤버의 프로필, 최신 목표, 챌린지 참여일 기준 D-day, 전체/최근 7일 통계를 반환한다.
- 자기 자신도 이 API로 조회할 수 있다.
- 친구 프로필 화면에서는 응답 중 `currentGoals`를 숨기거나 노출할지 FE 화면정의서에 따른다.
- 목표 변경 가능 여부는 반환하지 않는다. 내 목표 변경 가능 상태는 `GET /me/usage-goal-times/current`를 사용한다.

### Response

```json
{
  "id": 100,
  "userId": 1,
  "groupId": 1,
  "displayName": "의진",
  "profileImageUrl": "https://example.com/profile.png",
  "role": "MEMBER",
  "status": "ACTIVE",
  "joinedAt": "2026-05-01T23:50:00",
  "dayCount": 4,
  "isUserWithdrawn": false,
  "currentGoals": [
    {
      "id": 101,
      "usageGoalType": "TOTAL_USAGE",
      "goalMinutes": 60,
      "setAt": "2026-04-29T10:30:00"
    },
    {
      "id": 102,
      "usageGoalType": "INSTAGRAM",
      "goalMinutes": 30,
      "setAt": "2026-04-29T10:30:00"
    }
  ],
  "stats": {
    "overall": {
      "startDate": "2026-04-29",
      "endDate": "2026-05-05",
      "totalDays": 7,
      "achievedDays": 3,
      "achievementRate": 43
    },
    "recent7Days": {
      "startDate": "2026-04-29",
      "endDate": "2026-05-05",
      "totalDays": 7,
      "submittedDays": 5,
      "achievedDays": 3
    }
  }
}
```

### 필드 설명

| 필드 | 설명 |
| --- | --- |
| `id` | 그룹 멤버 ID |
| `userId` | 사용자 ID |
| `groupId` | 그룹 ID |
| `displayName` | 사용자 표시 이름 |
| `profileImageUrl` | 저장된 프로필 이미지 object key를 읽기 URL로 변환한 값 |
| `role` | 그룹 내 역할 |
| `status` | 그룹 멤버 상태 |
| `joinedAt` | 그룹 참여 일시 |
| `dayCount` | 현재 챌린지 참여일 기준 D-day. 같은 날이면 `0` |
| `isUserWithdrawn` | 해당 사용자가 회원 탈퇴한 사용자인지 여부 |
| `currentGoals` | 목표 타입별 최신 목표 시간 |
| `stats` | 목표 달성 통계 |

### 예외

| 상태 | 코드 | 조건 |
| --- | --- | --- |
| 404 | `GROUP_NOT_FOUND` | `groupId`에 해당하는 그룹이 없음 |
| 403 | `GROUP_ACCESS_DENIED` | 로그인 사용자가 해당 그룹의 활성 멤버가 아님 |
| 404 | `GROUP_MEMBER_NOT_FOUND` | 대상 멤버가 해당 그룹의 멤버 이력에 없음 |

---

## 2. GET /me/usage-goal-times/current

로그인 사용자의 목표 타입별 최신 목표 시간과 목표 변경 가능 상태를 조회한다.

### 동작

- 목표 타입별 최신 목표 시간을 반환한다.
- 마지막 목표 설정일 기준 7일이 지나야 목표를 다시 변경할 수 있다.
- 목표 설정 이력이 없으면 `goals`는 빈 배열이고, `changeAvailability.canChange`는 `true`이다.
- 목표 변경 가능 여부는 서버 기준 시각으로 계산한다.

### Response

```json
{
  "goals": [
    {
      "id": 101,
      "usageGoalType": "TOTAL_USAGE",
      "goalMinutes": 60,
      "setAt": "2026-04-29T10:30:00"
    },
    {
      "id": 102,
      "usageGoalType": "INSTAGRAM",
      "goalMinutes": 30,
      "setAt": "2026-04-29T10:30:00"
    }
  ],
  "changeAvailability": {
    "canChange": false,
    "nextChangeAvailableAt": "2026-05-06T10:30:00",
    "remainingDays": 1
  }
}
```

### 필드 설명

| 필드 | 설명 |
| --- | --- |
| `goals` | 목표 타입별 최신 목표 시간 |
| `changeAvailability` | 목표 변경 가능 상태 |

### 예외

| 상태 | 코드 | 조건 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 인증되지 않은 요청 |

---

## 3. PATCH /users/me

로그인 사용자의 프로필 정보를 부분 수정한다.

### 동작

- `displayName`, `profileImageObjectKey` 중 전달된 필드만 수정한다.
- `id`는 요청 본문으로 받지 않는다. 수정 대상은 access token의 사용자로 결정한다.
- `profileImageObjectKey`는 `POST /uploads/presigned-urls`에서 `uploadPurpose=PROFILE_IMAGE`로 발급받은 `objectKey`를 사용한다.
- 서버는 `profileImageObjectKey`가 `profile-images/{userId}/` prefix에 속하는지 검증한다.
- 서버는 저장된 object key를 읽기용 URL로 변환해 `profileImageUrl`로 응답한다.
- `profileImageObjectKey`를 `null`로 보내는 프로필 이미지 제거는 MVP 범위에서 제외한다.

### 프로필 이미지 변경 흐름

1. FE가 `POST /uploads/presigned-urls`를 호출한다.
2. 요청에는 `uploadPurpose=PROFILE_IMAGE`를 사용한다.
3. 서버는 `uploadUrl`, `objectKey`를 반환한다.
4. FE가 `uploadUrl`로 이미지를 직접 업로드한다.
5. FE가 `PATCH /users/me` 요청에 `profileImageObjectKey`를 담아 최종 저장한다.

### Request

```json
{
  "displayName": "의진",
  "profileImageObjectKey": "profile-images/1/550e8400-e29b-41d4-a716-446655440000-profile.png"
}
```

### Response

```json
{
  "id": 1,
  "displayName": "의진",
  "profileImageUrl": "https://media.detoxmate.co.kr/profile-images/1/550e8400-e29b-41d4-a716-446655440000-profile.png"
}
```

### 필드 설명

| 필드 | 설명 |
| --- | --- |
| `displayName` | 변경할 사용자 표시 이름 |
| `profileImageObjectKey` | 프로필 이미지 S3 object key |
| `profileImageUrl` | 클라이언트가 이미지를 읽을 때 사용할 URL |

### 예외

| 상태 | 코드 | 조건 |
| --- | --- | --- |
| 400 | `INVALID_DISPLAY_NAME` | 표시 이름이 정책에 맞지 않음 |
| 400 | `INVALID_PROFILE_IMAGE_OBJECT_KEY` | object key가 비어 있거나 허용 prefix가 아님 |
| 401 | `UNAUTHORIZED` | 인증되지 않은 요청 |

---

## 4. DELETE /groups/{groupId}/members/me

로그인 사용자가 해당 그룹에서 탈퇴한다.

### Path Variables

| 이름 | 설명 |
| --- | --- |
| `groupId` | 탈퇴할 그룹 ID |

### 동작

- 로그인 사용자가 `groupId` 그룹의 활성 멤버인지 확인한다.
- 활성 멤버이면 `GroupMember`를 `LEFT` 상태로 변경한다.
- 최신 챌린지 참가자가 있으면 `GroupChallengeParticipant`를 `WITHDRAWN` 상태로 변경한다.
- 과거 챌린지는 이미 정상 수행한 이력으로 보고 변경하지 않는다.
- 기존 `POST /groups/{id}/leave`는 이 API로 대체한다.

### 구현 상태

- `DELETE /groups/{groupId}/members/me`는 `GroupService.withdrawGroup(groupId, userId)`를 호출한다.
- `withdrawGroup`과 기존 `leaveGroup(groupId, userId)`는 동일한 private 탈퇴 흐름을 사용한다.
- `group_members.left_at`을 현재 시각으로 채우고 `status`를 `LEFT`로 변경한다.
- 최신 `group_challenges`에 연결된 `group_challenge_participants.withdrawn_at`을 현재 시각으로 채우고 `status`를 `WITHDRAWN`으로 변경한다.
- 최신 챌린지 기준은 `GroupChallengeService.getLatestChallenge(groupId)`와 동일하다.
- 그룹장이 탈퇴하면 남은 활성 멤버 중 최신 멤버를 그룹장으로 승격한다.
- 남은 활성 멤버가 없으면 최신 챌린지를 `CANCELED` 처리한다.
- `GroupServiceTest`, `GroupControllerTest`에서 탈퇴 API가 204를 반환하고 실제 탈퇴 흐름을 호출하는지 검증한다.

이번 PR에서 다루지 않는 항목:

- 인증 생성 API에서 `WITHDRAWN` 참가자 인증 생성을 막는 권한 검증은 activity record 영역에서 별도 처리한다.
- 챌린지 조회 권한 조건 변경은 group challenge 조회 영역에서 별도 처리한다.
- 그룹/멤버 목록에서 `LEFT` 멤버를 숨기는 필터링은 목록 조회 영역에서 별도 처리한다.
- 과거 챌린지 participant 상태는 변경하지 않는다.

### Response

```http
204 No Content
```

### 예외

| 상태 | 코드 | 조건 |
| --- | --- | --- |
| 404 | `GROUP_NOT_FOUND` | `groupId`에 해당하는 그룹이 없음 |
| 403 | `GROUP_ACCESS_DENIED` | 로그인 사용자가 해당 그룹의 활성 멤버가 아님 |
| 401 | `UNAUTHORIZED` | 인증되지 않은 요청 |

---

## 5. DELETE /users/me

로그인 사용자가 회원 탈퇴한다.

### 동작

- 수정 대상은 access token의 사용자로 결정한다.
- 소셜 로그인 연결 정보를 삭제한다.
- refresh token/session 정보를 삭제한다.
- 사용자 row는 삭제하지 않고 `WITHDRAWN` 상태로 변경한다.
- 탈퇴 일시를 기록한다.
- 공개 프로필은 `탈퇴한 사용자`, `profileImageUrl=null`로 익명화한다.
- 활성 그룹이 있으면 회원 탈퇴 전에 그룹 탈퇴와 동일한 처리를 수행한다.
- FCM token을 삭제한다.

### Response

```http
204 No Content
```

### 예외

| 상태 | 코드 | 조건 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 인증되지 않은 요청 |

---

## 6. PATCH /groups/{groupId}

그룹 이름을 변경한다.

### Path Variables

| 이름 | 설명 |
| --- | --- |
| `groupId` | 변경할 그룹 ID |

### 동작

- 로그인 사용자가 `groupId` 그룹의 활성 멤버인지 확인한다.
- MVP 기준으로 그룹 이름 변경은 그룹의 전역 `name`을 변경한다.
- 개인별 그룹 별칭은 MVP 범위에서 제외한다. 추후 필요하면 `PATCH /groups/{groupId}/my-settings`로 분리한다.
- 그룹 이름은 기존 그룹 생성 정책과 동일하게 공백 포함 12자 이내로 제한한다.

### Request

```json
{
  "name": "주말 디톡스"
}
```

### Response

```json
{
  "id": 1,
  "inviteCode": "AB123",
  "name": "주말 디톡스",
  "myRole": "OWNER",
  "members": [
    {
      "id": 100,
      "userId": 1,
      "displayName": "지민",
      "profileImageUrl": "https://...",
      "role": "OWNER",
      "status": "ACTIVE",
      "joinedAt": "2026-04-19T10:00:00",
      "leftAt": null
    }
  ],
  "currentChallenge": {
    "id": 10,
    "challengeNo": 1,
    "status": "RECRUITING",
    "startAt": null,
    "endAt": null
  },
  "createdAt": "2026-04-19T10:00:00",
  "updatedAt": "2026-05-05T10:00:00"
}
```

### 예외

| 상태 | 코드 | 조건 |
| --- | --- | --- |
| 400 | `INVALID_GROUP_NAME` | 그룹 이름이 비어 있거나 12자를 초과함 |
| 404 | `GROUP_NOT_FOUND` | `groupId`에 해당하는 그룹이 없음 |
| 403 | `GROUP_ACCESS_DENIED` | 로그인 사용자가 해당 그룹의 활성 멤버가 아님 |
| 401 | `UNAUTHORIZED` | 인증되지 않은 요청 |

---

## 집계 규칙

### 날짜 기준

- 서비스 기준 시간대는 `Asia/Seoul`이다.
- 일별 통계는 `LocalDate` 기준으로 계산한다.
- `recent7Days`는 오늘을 포함한 최근 7일이다.
- 예: 2026-05-05 조회 시 `startDate=2026-04-29`, `endDate=2026-05-05`.

### D-day

- 현재 챌린지 참가자의 `joinedAt` 날짜를 기준으로 한다.
- 계산식: `조회일 - 참가일`
- 같은 날 참가하고 같은 날 조회하면 `0`이다.

### 전체 달성률

- 기본 시작일은 최신 `TOTAL_USAGE` 목표 설정일이다.
- `TOTAL_USAGE` 목표가 없으면 현재 인증에 사용하는 목표 타입 중 가장 최근 목표 설정일을 사용한다.
- 목표 설정 이력이 없으면 `overall.totalDays=0`, `overall.achievedDays=0`, `overall.achievementRate=0`을 반환한다.
- 달성률은 `achievedDays / totalDays * 100`을 정수 반올림한다.

### 최근 7일 통계

- `recent7Days.totalDays`는 항상 `7`이다.
- 오늘 인증하지 않았으면 오늘은 분모에는 포함되고 `submittedDays`, `achievedDays`에는 포함되지 않는다.
- 같은 날짜에 여러 인증이 있으면 최신 최종 인증 1개만 집계한다.

### 목표 변경 가능 시점

- 마지막 목표 설정 일시의 7일 뒤를 `nextChangeAvailableAt`으로 본다.
- 현재 시각이 `nextChangeAvailableAt` 이상이면 `canChange=true`, `nextChangeAvailableAt=null`, `remainingDays=0`을 반환한다.

---

## 기능 구현 체크리스트

이 체크리스트는 API 문서/DTO/Controller 생성 여부가 아니라, 실제 사용자가 API를 호출했을 때 기능이 동작하는지를 기준으로 한다.

### 1. GET /groups/{groupId}/members/{memberId}

현재 구현률: 100%

- [x] API path/response contract 확정
- [x] 인증된 사용자만 접근하는 형태
- [x] 같은 그룹 멤버 프로필 응답 형태 문서화
- [x] 요청자가 해당 그룹의 활성 멤버인지 확인
- [x] 조회 대상 `memberId`가 해당 그룹의 멤버 이력에 있는지 확인
- [x] 회원 탈퇴한 대상 멤버 프로필 익명화
- [x] 멤버 기본 정보 조회
- [x] 현재 목표 시간 목록 조회
- [x] D+ 계산
- [x] 전체 달성률 계산
- [x] 최근 7일 인증/달성 통계 계산
- [x] 403/404 에러 정책 실제 적용

현재 상태:

- 실제 조회 기능이 구현되어 있다.
- 대상 멤버는 활성 멤버뿐 아니라 같은 그룹의 과거 멤버까지 조회할 수 있다.

### 2. PATCH /users/me

현재 구현률: 80%

- [x] API path/request/response contract 확정
- [x] `id`를 request body에서 받지 않도록 정리
- [x] `displayName`, `profileImageObjectKey` 부분 수정 형태 확정
- [x] displayName 변경
- [x] profileImageObjectKey 저장
- [x] object key가 `profile-images/{userId}/` prefix인지 검증
- [x] 저장된 object key를 읽기 URL로 변환
- [ ] 프로필 이미지 제거 정책 결정 및 처리
- [x] 닉네임 유효성 정책 실제 적용

현재 상태:

- 실제 프로필 부분 수정 기능이 구현되어 있다.
- 프로필 이미지 제거는 MVP 범위에서 제외되어 있다.

### 3. DELETE /groups/{groupId}/members/me

현재 구현률: 100%

- [x] API path 확정
- [x] 인증된 사용자 기준 endpoint 형태 확정
- [x] 204 응답 contract 문서화
- [x] 그룹 존재 여부 확인
- [x] 내가 해당 그룹의 활성 멤버인지 확인
- [x] `GroupMember`를 `LEFT` 처리
- [x] `GroupMember.leftAt` 기록
- [x] 최신 챌린지 참가 상태를 `WITHDRAWN` 처리
- [x] 최신 챌린지 참가자의 `withdrawnAt` 기록
- [x] 과거 챌린지 참가 상태는 변경하지 않음
- [x] `DELETE /groups/{groupId}/members/me`가 실제 탈퇴 흐름을 호출하도록 연결
- [x] 기존 `leaveGroup` 로직과 중복되지 않도록 공통화
- [x] 탈퇴 후 그룹이 0명일 때 최신 챌린지 `CANCELED` 처리
- [x] 회원 탈퇴와 동일한 그룹 이탈 흐름으로 재사용
- [x] 인증 생성 API 권한 검증은 이번 PR 범위에서 제외
- [x] 챌린지 조회 권한 조건 변경은 이번 PR 범위에서 제외
- [x] 그룹/멤버 목록 필터링 변경은 이번 PR 범위에서 제외

현재 상태:

- 실제 그룹 탈퇴 기능이 API에 연결되어 있다.
- 회원 탈퇴도 같은 그룹 이탈 흐름을 재사용한다.

### 4. DELETE /users/me

현재 구현률: 100%

- [x] dev 전용이 아니라 정식 API contract로 정리
- [x] 인증된 사용자 기준 endpoint 형태 확정
- [x] 204 응답 contract 문서화
- [x] 소셜 로그인 연결 정보 삭제
- [x] refresh token/session 삭제
- [x] user를 물리 삭제하지 않고 `WITHDRAWN` 상태로 변경
- [x] 탈퇴 사용자 공개 프로필 익명화
- [x] 활성 그룹이 있을 때 그룹 탈퇴와 동일한 처리 수행
- [x] FCM token 삭제 처리
- [x] 이미 탈퇴한 사용자 케이스는 멱등 종료
- [x] 과거 피드/댓글/그룹 이력 보존 정책 정리

현재 상태:

- 실제 회원 탈퇴 기능이 API에 연결되어 있다.

### 5. PATCH /groups/{groupId}

현재 구현률: 15%

- [x] API path/request/response contract 확정
- [x] 전역 `groups.name` 변경으로 MVP 범위 정리
- [x] 개인별 그룹 별칭은 제외로 정리
- [x] 그룹명 12자 제한 request 검증 형태 추가
- [ ] 요청자가 해당 그룹의 활성 멤버인지 확인
- [ ] 그룹 존재 여부 확인
- [ ] 그룹명 실제 변경
- [ ] 변경된 그룹 상세 응답 반환
- [ ] 빈 문자열/공백/중복 정책 정리
- [ ] 개인별 그룹명 변경이 필요해질 경우 별도 API로 분리

현재 상태:

- mock API 계약만 있다.
- 실제 변경 기능은 아직 구현되지 않았다.
