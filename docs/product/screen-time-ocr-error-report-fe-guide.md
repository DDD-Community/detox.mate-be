> 작성자: Codex
> 작성일: 2026-05-13
> 상태: FE 전달용

---

# 스크린타임 OCR 오류 신고 FE 연동 가이드

이 문서는 FE에서 스크린타임 OCR 오류 신고 기능을 붙일 때 필요한 사용 흐름만 정리한다.
필드별 상세 schema, required 여부, 응답 타입은 Swagger UI의 `Upload API`, `Screen Time OCR Error Report API`를 기준으로 확인한다.

## 사용 시나리오

사용자가 스크린타임 인증 과정에서 OCR이 읽은 총 사용 시간이 실제 스크린샷과 다르다고 판단하면, FE는 "시간이 틀려요" 같은 액션으로 오류 신고를 생성한다.

신고가 생성되면 BE가 관리자 검수용 Discord 알림을 보낸다. FE는 신고 생성 성공 이후 별도 검수 상태를 추적하지 않는다.

## 호출 흐름

### 1. OCR 오류 신고용 이미지 업로드 URL 발급

기존 presigned URL API를 사용한다.

```http
POST /uploads/presigned-urls
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청의 `uploadPurpose`는 반드시 `SCREEN_TIME_OCR_REPORT_IMAGE`를 사용한다.

```json
{
  "fileName": "screen-time.png",
  "contentType": "image/png",
  "fileSize": 123456,
  "uploadPurpose": "SCREEN_TIME_OCR_REPORT_IMAGE"
}
```

응답의 `uploadUrl`로 이미지를 업로드하고, `objectKey`는 다음 단계의 `imageObjectKey`로 그대로 사용한다.

### 2. S3에 이미지 업로드

1단계 응답의 `uploadUrl`로 파일을 `PUT` 업로드한다.

- `Content-Type`은 presigned URL 발급 요청에 넣은 `contentType`과 동일하게 보낸다.
- 지원 이미지 타입은 `image/png`, `image/jpeg`, `image/webp`, `image/heic`, `image/heif`이다.
- 업로드 완료 후 BE에 별도 업로드 완료 API를 호출하지 않는다.

### 3. OCR 오류 신고 생성

```http
POST /screen-time-ocr-error-reports
Authorization: Bearer {accessToken}
Content-Type: application/json
```

예시:

```json
{
  "groupChallengeParticipantId": 10,
  "recordDate": "2026-05-12",
  "imageObjectKey": "screen-time-ocr-reports/1/2026/05/sample.png",
  "ocrTotalUsedMinutes": 180
}
```

각 값의 출처:

| 값 | 출처 |
| --- | --- |
| `groupChallengeParticipantId` | 현재 인증하려는 그룹 챌린지 참여자 ID |
| `recordDate` | 사용자가 인증하려는 날짜 |
| `imageObjectKey` | 1단계 presigned URL 응답의 `objectKey` |
| `ocrTotalUsedMinutes` | FE/OCR이 기존에 계산한 총 사용 시간(분) |
| `activityRecordId` | 선택값. 일반적인 신고 플로우에서는 보내지 않아도 됨 |

성공 응답은 신고 접수 완료로 처리한다.

```json
{
  "id": 555,
  "status": "PENDING",
  "createdAt": "2026-05-12T21:31:00"
}
```
