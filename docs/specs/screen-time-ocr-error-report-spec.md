# Screen Time OCR Error Report Spec

## Purpose

스크린타임 인증 과정에서 OCR이 추론한 총 사용 시간이 실제 이미지와 다를 때, 사용자가 "시간이 틀려요"로 신고할 수 있게 한다. 서버는 신고 이미지, OCR이 추론한 총 사용 시간, 날짜, 유저 정보를 저장하고, admin이 추후 검수하여 인증 시간을 수정하거나 반려한다.

## Scope

이번 범위는 단순한 1:1 구조로 제한한다.

- 신고 테이블은 1개만 만든다.
- 수정 대상 시간은 `TOTAL_USAGE` 1개만 둔다.
- admin API는 목록 조회와 업데이트 2개만 만든다.
- admin 업데이트 API는 request body의 `action`으로 수정/반려를 구분한다.
- 별도 상세 조회 API는 만들지 않는다. 목록 응답에 검수에 필요한 정보를 포함한다.

## User Flow

1. 클라이언트가 OCR 오류 신고용 presigned URL을 발급받는다.
2. 클라이언트가 스크린타임 이미지를 S3에 업로드한다.
3. 사용자가 "시간이 틀려요" 버튼을 누르면 클라이언트가 서버에 OCR 오류 신고를 생성한다.
4. admin이 신고 목록에서 이미지를 확인한다.
5. admin이 총 사용 시간을 수정하거나 신고를 반려한다.

## Upload Purpose

`UploadPurpose`에 다음 값을 추가한다.

```java
SCREEN_TIME_OCR_REPORT_IMAGE
```

Object key prefix:

```text
screen-time-ocr-reports/{userId}/{yyyy}/{MM}/{uuid}-{fileName}
```

예시:

```text
screen-time-ocr-reports/1/2026/05/7f0a...-screen-time.png
```

신고 생성 시 `imageObjectKey`는 현재 로그인 유저의 `SCREEN_TIME_OCR_REPORT_IMAGE` prefix에 속해야 한다.

## Database

### Table: `screen_time_ocr_error_report`

| Column | Type | Null | Description |
|---|---:|---:|---|
| `screen_time_ocr_error_report_id` | BIGINT | N | PK |
| `user_id` | BIGINT | N | 신고한 유저 |
| `activity_record_id` | BIGINT | Y | 이미 생성된 인증 기록. 인증 후 신고라면 사용 |
| `group_challenge_participant_id` | BIGINT | Y | 챌린지 참여자. 인증 전 신고까지 허용할 경우 사용 |
| `record_date` | DATE | N | 인증 대상 날짜 |
| `image_object_key` | VARCHAR(1024) | N | 스크린타임 이미지 S3 object key |
| `ocr_total_used_minutes` | INT | N | OCR이 추론한 총 사용 시간 |
| `corrected_total_used_minutes` | INT | Y | admin이 수정한 총 사용 시간 |
| `status` | VARCHAR(30) | N | `PENDING`, `CORRECTED`, `REJECTED` |
| `admin_note` | TEXT | Y | admin 처리 메모 |
| `resolved_by_user_id` | BIGINT | Y | 처리한 admin user id |
| `resolved_at` | DATETIME | Y | 처리 시각 |
| `created_at` | DATETIME | N | 생성 시각 |
| `updated_at` | DATETIME | N | 수정 시각 |

### Constraints

- `ocr_total_used_minutes >= 0`
- `corrected_total_used_minutes IS NULL OR corrected_total_used_minutes >= 0`
- `status IN ('PENDING', 'CORRECTED', 'REJECTED')`

### Indexes

- `idx_screen_time_ocr_report_status_created_at(status, created_at)`
- `idx_screen_time_ocr_report_user_created_at(user_id, created_at)`
- `idx_screen_time_ocr_report_activity_record(activity_record_id)`

## User API

### Create OCR Error Report

```http
POST /screen-time-ocr-error-reports
Authorization: Bearer {accessToken}
Content-Type: application/json
```

Request:

```json
{
  "activityRecordId": 123,
  "groupChallengeParticipantId": 10,
  "recordDate": "2026-05-12",
  "imageObjectKey": "screen-time-ocr-reports/1/2026/05/sample.png",
  "ocrTotalUsedMinutes": 180
}
```

Response: `201 Created`

```json
{
  "id": 555,
  "status": "PENDING",
  "createdAt": "2026-05-12T21:31:00"
}
```

Validation:

- 로그인 유저만 호출할 수 있다.
- `recordDate`는 필수다.
- `imageObjectKey`는 필수이며 현재 유저 소유 prefix여야 한다.
- `ocrTotalUsedMinutes`는 0 이상이어야 한다.
- `activityRecordId`가 있으면 해당 `ActivityRecord.user.id`가 현재 유저와 같아야 한다.
- `groupChallengeParticipantId`가 있으면 현재 유저가 활성 참여자인지 확인한다.
- `activityRecordId`와 `groupChallengeParticipantId`가 모두 있으면 같은 참여자 맥락이어야 한다.

## Admin API

Admin API는 2개만 만든다.

### List Reports

```http
GET /admin/screen-time-ocr-error-reports?status=PENDING&page=0&size=20
Authorization: Bearer {accessToken}
```

Query parameters:

| Name | Required | Description |
|---|---:|---|
| `status` | N | `PENDING`, `CORRECTED`, `REJECTED`. 기본값은 `PENDING` |
| `page` | N | 0-base page. 기본값 `0` |
| `size` | N | page size. 기본값 `20` |

Response: `200 OK`

```json
{
  "items": [
    {
      "id": 555,
      "userId": 1,
      "userDisplayName": "지민",
      "activityRecordId": 123,
      "groupChallengeParticipantId": 10,
      "recordDate": "2026-05-12",
      "imageUrl": "https://example.com/media/screen-time-ocr-reports/1/2026/05/sample.png",
      "ocrTotalUsedMinutes": 180,
      "correctedTotalUsedMinutes": null,
      "status": "PENDING",
      "adminNote": null,
      "resolvedByUserId": null,
      "resolvedAt": null,
      "createdAt": "2026-05-12T21:31:00",
      "updatedAt": "2026-05-12T21:31:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Notes:

- 별도 상세 조회 API를 만들지 않으므로 목록 응답에 이미지 URL과 OCR 총 사용 시간, 연결된 인증 정보를 포함한다.
- `imageUrl`은 저장된 `imageObjectKey`를 `ImageReadUrlBuilder`로 변환해서 내려준다.

### Update Report

```http
PATCH /admin/screen-time-ocr-error-reports/{reportId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

수정 request:

```json
{
  "action": "CORRECT",
  "correctedTotalUsedMinutes": 165,
  "adminNote": "스크린샷 기준 총 사용시간 2시간 45분"
}
```

반려 request:

```json
{
  "action": "REJECT",
  "adminNote": "OCR 오류를 확인할 수 없음"
}
```

Response: `200 OK`

```json
{
  "id": 555,
  "status": "CORRECTED",
  "ocrTotalUsedMinutes": 180,
  "correctedTotalUsedMinutes": 165,
  "adminNote": "스크린샷 기준 총 사용시간 2시간 45분",
  "resolvedByUserId": 99,
  "resolvedAt": "2026-05-13T10:00:00"
}
```

Validation:

- admin 권한이 있어야 한다.
- report는 `PENDING` 상태여야 한다.
- `action`은 `CORRECT` 또는 `REJECT`만 허용한다.
- `action = CORRECT`이면 `correctedTotalUsedMinutes`가 필수이며 0 이상이어야 한다.
- `action = CORRECT`이면 report에 `activityRecordId`가 있어야 한다.
- `action = REJECT`이면 인증 기록을 수정하지 않는다.

## Correction Behavior

`action = CORRECT`일 때:

1. report의 `corrected_total_used_minutes`를 저장한다.
2. report 상태를 `CORRECTED`로 변경한다.
3. `resolved_by_user_id`, `resolved_at`, `admin_note`를 저장한다.
4. 연결된 `ActivityRecord`에서 `TOTAL_USAGE` detail을 찾는다.
5. 해당 detail의 `useMinutes`를 `correctedTotalUsedMinutes`로 변경한다.
6. 해당 detail의 `achieved`를 연결된 목표 시간 기준으로 재계산한다.
7. `ActivityRecord`의 전체 detail 달성 여부를 다시 계산한다.
8. 연결된 `ChallengeRecord.status`를 다시 설정한다.
   - 전체 detail이 달성됨: `AFTER_RECORD_SUCCESS`
   - 하나라도 미달성: `AFTER_RECORD_FAIL`

주의:

- 이번 기능에서 admin이 직접 수정하는 값은 `TOTAL_USAGE` 하나뿐이다.
- 다만 최종 `ChallengeRecord.status`는 전체 detail 기준으로 재계산해야 한다.
- 예를 들어 `TOTAL_USAGE`가 성공으로 바뀌어도 `INSTAGRAM` detail이 실패라면 최종 상태는 `AFTER_RECORD_FAIL`이다.

`action = REJECT`일 때:

1. report 상태를 `REJECTED`로 변경한다.
2. `resolved_by_user_id`, `resolved_at`, `admin_note`를 저장한다.
3. `ActivityRecord`와 `ChallengeRecord`는 수정하지 않는다.

## Admin Authorization

현재 코드에는 admin 권한 모델이 없다. 구현 전에 admin 권한 방식을 정해야 한다.

권장안:

- `users`에 role 컬럼 추가
- enum: `USER`, `ADMIN`
- admin API 진입 시 `AdminAuthorizationService.requireAdmin(currentUser.id())` 호출

임시안:

- `app.admin.user-ids` 설정값으로 admin user id allowlist 관리
- 빠르게 만들 수 있지만 운영 실수 가능성이 있어 장기안으로는 권장하지 않는다.

## Implementation Tasks

1. `UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE` 추가
2. OCR 신고 이미지 object key prefix 추가
3. `ScreenTimeOcrErrorReport` entity 추가
4. `ScreenTimeOcrErrorReportStatus` enum 추가
5. repository, DTO, service, controller 추가
6. user 신고 생성 API 추가
7. admin list API 추가
8. admin update API 추가
9. `ActivityRecordDetail`에 총 사용 시간 수정 메서드 추가
10. `ChallengeRecord`에 admin 수정용 인증 결과 갱신 메서드 추가
11. 테스트 및 REST Docs/OpenAPI 문서 추가

## Test Cases

- presigned URL 발급 시 OCR 신고 이미지 prefix가 생성된다.
- 다른 유저의 OCR 신고 이미지 object key로 report 생성 시 거부된다.
- report 생성 시 `PENDING` 상태로 저장된다.
- admin list API가 `PENDING` report를 반환한다.
- non-admin은 admin API를 호출할 수 없다.
- admin `CORRECT` 시 `correctedTotalUsedMinutes`가 저장된다.
- admin `CORRECT` 시 `ActivityRecordDetail.useMinutes`가 수정된다.
- admin `CORRECT` 시 `ActivityRecordDetail.achieved`가 재계산된다.
- admin `CORRECT` 시 `ChallengeRecord.status`가 전체 detail 기준으로 재계산된다.
- admin `REJECT` 시 report만 `REJECTED`가 되고 인증 기록은 바뀌지 않는다.
