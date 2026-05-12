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
4. 서버는 신고 생성 커밋 후 Discord webhook으로 이미지, OCR 추측 시간, admin 수정/반려 curl을 알린다.
5. admin이 Discord 알림 또는 신고 목록에서 이미지를 확인한다.
6. admin이 총 사용 시간을 수정하거나 신고를 반려한다.

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
| `group_challenge_participant_id` | BIGINT | N | 인증 대상 그룹 챌린지 참여자 |
| `activity_record_id` | BIGINT | Y | 이미 생성된 인증 기록. 인증 후 신고라면 사용 |
| `record_date` | DATE | N | 인증 대상 날짜 |
| `image_object_key` | VARCHAR(1024) | N | 스크린타임 이미지 S3 object key |
| `ocr_total_used_minutes` | INT | N | OCR이 추론한 총 사용 시간 |
| `corrected_total_used_minutes` | INT | Y | admin이 수정한 총 사용 시간 |
| `status` | VARCHAR(30) | N | `PENDING`, `CORRECTED`, `REJECTED` |
| `admin_note` | TEXT | Y | admin 처리 메모 |
| `resolved_at` | DATETIME | Y | 처리 시각 |
| `created_at` | DATETIME | N | 생성 시각 |
| `updated_at` | DATETIME | N | 수정 시각 |

### Constraints

- `ocr_total_used_minutes >= 0`
- `corrected_total_used_minutes IS NULL OR corrected_total_used_minutes >= 0`
- `status IN ('PENDING', 'CORRECTED', 'REJECTED')`
- `group_challenge_participant_id`는 `group_challenge_participants.group_challenge_participant_id`를 참조한다.
- `activity_record_id`는 nullable FK이며, 값이 있으면 `activity_record.activity_record_id`를 참조한다.

### Indexes

- `idx_screen_time_ocr_report_status_created_at(status, created_at, screen_time_ocr_error_report_id)`
- `idx_screen_time_ocr_report_participant_record_date(group_challenge_participant_id, record_date)`
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
- `groupChallengeParticipantId`는 필수이며 현재 유저가 활성 참여자인지 확인한다.
- `activityRecordId`가 있으면 해당 `ActivityRecord.user.id`가 현재 유저와 같아야 한다.
- `activityRecordId`와 `groupChallengeParticipantId`가 모두 있으면 같은 참여자 맥락이어야 한다.

## Admin API

Admin API는 2개만 만든다.

### List Reports

```http
GET /admin/screen-time-ocr-error-reports?status=PENDING&page=0&size=20
X-Admin-Token: {adminReviewToken}
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
- 처리자는 별도 응답 필드로 내려주지 않는다. 처리 여부는 `status`, `adminNote`, `resolvedAt`으로 확인한다.

Error responses:

- admin token이 없거나 틀리면 `403 Forbidden`과 `ErrorResponse`를 반환한다.

### Update Report

```http
PATCH /admin/screen-time-ocr-error-reports/{reportId}
X-Admin-Token: {adminReviewToken}
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
  "resolvedAt": "2026-05-13T10:00:00"
}
```

Validation:

- `X-Admin-Token`이 서버의 `ADMIN_REVIEW_TOKEN`과 일치해야 한다.
- report는 `PENDING` 상태여야 한다.
- `action`은 `CORRECT` 또는 `REJECT`만 허용한다.
- `action = CORRECT`이면 `correctedTotalUsedMinutes`가 필수이며 0 이상이어야 한다.
- `action = CORRECT`이면 `activityRecordId`가 없더라도 `groupChallengeParticipantId + recordDate` 기준으로 인증 기록을 생성/연결한다.
- `action = REJECT`이면 인증 기록을 수정하지 않는다.

Error responses:

- admin token이 없거나 틀리면 `403 Forbidden`과 `ErrorResponse`를 반환한다.

## Correction Behavior

`action = CORRECT`일 때:

1. `groupChallengeParticipantId + recordDate` 기준으로 `ChallengeRecord`를 찾거나 생성한다.
2. `ChallengeRecord.activityRecordId`가 있으면 해당 `ActivityRecord`를 우선 사용한다.
3. `ChallengeRecord.activityRecordId`가 없고 report의 `activity_record_id`가 있으면 해당 `ActivityRecord`를 사용한다.
4. 둘 다 없으면 report의 참여자, 이미지, 최신 `TOTAL_USAGE` 목표로 `ActivityRecord`와 `TOTAL_USAGE` detail을 생성하고 report에 연결한다.
5. 연결된 `ActivityRecord`의 `TOTAL_USAGE` detail `useMinutes`를 `correctedTotalUsedMinutes`로 변경한다.
6. 해당 detail의 `achieved`를 연결된 목표 시간 기준으로 재계산한다.
7. `ActivityRecord`의 전체 detail 달성 여부를 다시 계산한다.
8. 연결된 `ChallengeRecord.status`를 다시 설정한다.
   - 전체 detail이 달성됨: `AFTER_RECORD_SUCCESS`
   - 하나라도 미달성: `AFTER_RECORD_FAIL`
9. report의 `corrected_total_used_minutes`, `status = CORRECTED`, `resolved_at`, `admin_note`를 저장한다.

주의:

- 이번 기능에서 admin이 직접 수정하는 값은 `TOTAL_USAGE` 하나뿐이다.
- 기존 `ActivityRecord`에 `TOTAL_USAGE` detail이 없으면 수정할 수 없다.
- 다만 최종 `ChallengeRecord.status`는 전체 detail 기준으로 재계산해야 한다.
- 예를 들어 `TOTAL_USAGE`가 성공으로 바뀌어도 `INSTAGRAM` detail이 실패라면 최종 상태는 `AFTER_RECORD_FAIL`이다.

`action = REJECT`일 때:

1. report 상태를 `REJECTED`로 변경한다.
2. `resolved_at`, `admin_note`를 저장한다.
3. `ActivityRecord`와 `ChallengeRecord`는 수정하지 않는다.

## Admin Authorization

MVP에서는 별도 admin 유저 체계를 만들지 않고 `X-Admin-Token` 공유 토큰으로 보호한다.

- 서버 설정: `ADMIN_REVIEW_TOKEN`
- admin API 진입 시 `AdminAuthorizationService.requireAdmin(adminToken)` 호출
- 토큰이 설정되지 않으면 admin API는 열지 않고 `500`을 반환한다.
- 토큰이 없거나 틀리면 `403`을 반환한다.

## Discord Notification

OCR 오류 신고가 생성되면 DB commit 이후 Discord webhook 알림을 전송한다.

- 설정 property: `app.discord.ocr-report.webhook-url`
- webhook URL env: `DISCORD_OCR_REPORT_WEBHOOK_URL`
- curl base URL property: `app.discord.ocr-report.api-base-url`
- curl base URL env: `APP_PUBLIC_BASE_URL`
- webhook URL이 비어 있으면 알림을 보내지 않는다.
- Discord 전송 실패는 사용자 신고 생성 응답을 실패시키지 않고 로그만 남긴다.
- 메시지에는 이미지, OCR 추측 총 사용 시간, report id, 수정/반려 admin API curl을 포함한다.
- `APP_PUBLIC_BASE_URL`이 비어 있으면 curl URL에는 `$APP_PUBLIC_BASE_URL` placeholder를 사용한다.
- 메시지에 실제 `ADMIN_REVIEW_TOKEN` 값은 포함하지 않고 `$ADMIN_REVIEW_TOKEN` placeholder만 포함한다.

## Implemented Components

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
11. Discord webhook 공용 client와 OCR 오류 신고 알림 adapter 추가
12. 테스트 및 REST Docs/OpenAPI 문서 추가

## Test Cases

- presigned URL 발급 시 OCR 신고 이미지 prefix가 생성된다.
- 다른 유저의 OCR 신고 이미지 object key로 report 생성 시 거부된다.
- report 생성 시 `PENDING` 상태로 저장된다.
- admin list API가 `PENDING` report를 반환한다.
- non-admin은 admin API를 호출할 수 없다.
- admin token이 틀리면 admin list/update REST Docs와 OpenAPI에 `403 ErrorResponse`가 포함된다.
- admin `CORRECT` 시 `correctedTotalUsedMinutes`가 저장된다.
- admin `CORRECT` 시 `ActivityRecordDetail.useMinutes`가 수정된다.
- admin `CORRECT` 시 `ActivityRecordDetail.achieved`가 재계산된다.
- admin `CORRECT` 시 `ChallengeRecord.status`가 전체 detail 기준으로 재계산된다.
- admin `CORRECT` 시 기존 인증 기록이 없으면 `ActivityRecord`를 생성하고 report에 연결한다.
- admin `REJECT` 시 report만 `REJECTED`가 되고 인증 기록은 바뀌지 않는다.
- report 생성 commit 이후 Discord 알림이 전송된다.
- report 생성 rollback 시 Discord 알림이 전송되지 않는다.
- Discord webhook URL이 비어 있으면 알림을 전송하지 않는다.
- Discord 메시지에 실제 admin token 값은 포함하지 않는다.
- REST Docs/OpenAPI 결과에 처리자 전용 필드가 남아 있지 않다.
