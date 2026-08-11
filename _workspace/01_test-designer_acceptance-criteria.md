# Acceptance Criteria

## Normal
- 인증 사용자의 ID와 요청 `userId`가 같고 `appDisplayName`과 `dailyLimitMinutes`가 유효하면 `POST /me/apps`는 `201 Created`와 생성된 `appId`, `userId`, `appDisplayName`, `dailyLimitMinutes`를 반환하고, `App`과 소유된 `AppTimeLimit`을 하나의 aggregate로 함께 저장한다.
- `GET /me/apps`는 `200 OK`와 인증 사용자가 소유한 앱만 담은 목록을 반환하며 다른 사용자의 앱을 노출하지 않는다.
- `GET /me/apps/{appId}`는 인증 사용자가 소유한 앱이면 `200 OK`와 해당 앱의 ID, 소유자 ID, 표시 이름, 일일 제한 시간을 반환한다.
- 인증 사용자의 ID와 요청 `userId`가 같고 대상 앱을 소유하면 `PUT /me/apps/{appId}`는 `200 OK`와 변경 결과를 반환한다. 기존 `appId`와 소유자는 유지되고 표시 이름과 기존 `AppTimeLimit`의 제한 시간만 변경된다.
- 인증 사용자가 소유한 앱을 `DELETE /me/apps/{appId}`로 삭제하면 `204 No Content`를 반환하고 `App`과 그 `AppTimeLimit`이 함께 삭제된다.

## Failure
- 인증 정보가 없거나 유효하지 않은 모든 App lock API 요청은 `401 Unauthorized`이고 저장 상태를 변경하지 않는다.
- 생성 또는 수정 요청의 `userId`가 `CurrentUser.id()`와 다르면 `403 Forbidden`이고 앱이나 제한 시간을 생성·수정하지 않는다.
- 인증 토큰의 사용자 ID에 해당하는 사용자가 존재하지 않으면 기존 인증 계약에 따라 컨트롤러 진입 전에 `401 Unauthorized`이고 저장 상태를 변경하지 않는다.
- 앱이 없거나 다른 사용자가 소유한 앱에 대한 단건 조회·수정·삭제는 모두 `404 Not Found`이다. 두 경우를 같은 응답으로 처리해 타인의 앱 존재 여부를 노출하지 않으며 저장 상태도 변경하지 않는다.
- 생성·수정 요청에서 `userId` 또는 `dailyLimitMinutes`가 누락/null이거나, `appDisplayName`이 누락/null/blank이거나, 필드 값이 허용 범위를 벗어나면 `400 Bad Request`이고 저장 상태를 변경하지 않는다.

## Boundary
- `dailyLimitMinutes`의 최솟값 `0`과 최댓값 `1440`은 생성과 수정에서 허용한다.
- `dailyLimitMinutes`가 `-1` 또는 `1441`이면 생성과 수정에서 `400 Bad Request`이다.
- 길이 1과 100의 non-blank `appDisplayName`은 허용한다.
- 빈 문자열, 공백만 있는 문자열, 길이 101의 `appDisplayName`은 생성과 수정에서 `400 Bad Request`이다.
- 소유 앱이 없을 때 `GET /me/apps`는 `200 OK`와 빈 `apps` 목록을 반환한다.
- 동일한 값으로 수정해도 새 `App` 또는 두 번째 `AppTimeLimit`을 만들지 않고 기존 aggregate를 유지한다.
- 목록 순서는 요구사항에 없으므로 테스트는 특정 정렬 순서에 결합하지 않는다.

## Regression
- 기존 `User` aggregate와 `users` 테이블을 재사용한다. App 생성 시 User를 중복 생성하지 않고 App 삭제 시 User를 삭제하지 않는다.
- `App`만 aggregate root와 repository 진입점으로 사용하고, `AppTimeLimit` 전용 controller/service/repository API를 노출하지 않는다.
- 목록·단건 조회·삭제는 요청에서 `userId`를 받지 않고 오직 `CurrentUser.id()`로 소유권을 제한한다.
- 수정은 기존 `AppTimeLimit`을 갱신하며 child row를 누적하지 않고, App 삭제는 cascade/orphan-removal 계약에 따라 child row도 남기지 않는다.
- App lock 변경 후 기존 전체 테스트가 계속 통과한다.

# RED Test Plan
- target test file: `src/test/java/com/detoxmate/applock/controller/AppControllerTest.java`
- target bounded context: `applock`
- target aggregate: `App` (owned child: `AppTimeLimit`, external aggregate reference: `User`)
- expected production package: 기존 legacy-compatible 구조인 `com.detoxmate.applock.controller|dto|service|domain|repository`; broad package migration 없음
- test name: `create_returnsCreatedApp_whenAuthenticatedRequestIsValid`
- observable behavior: 저장된 사용자의 access token으로 `POST /me/apps`에 동일한 `userId`, 유효한 이름, 유효한 제한 시간을 보내면 `201 Created`와 생성된 App 표현을 반환한다.
- setup data: 실제 `UserRepository`로 사용자 1명을 저장하고 기존 `JwtTokenProvider`로 access token을 생성한다. 요청 본문은 `{"userId": <savedUserId>, "appDisplayName": "Instagram", "dailyLimitMinutes": 60}`을 사용한다. DTO나 아직 없는 domain factory를 테스트 코드에서 직접 호출하지 않는 `@SpringBootTest` + `@AutoConfigureMockMvc` 통합 테스트로 작성해 현재 스켈레톤에서도 컴파일되게 한다.
- command: `./gradlew test --tests "com.detoxmate.applock.controller.AppControllerTest.create_returnsCreatedApp_whenAuthenticatedRequestIsValid"`
- follow-up command: `./gradlew test --tests "com.detoxmate.applock.*"`
- regression command: `./gradlew test`

# RED Result
- command: `./gradlew test --tests "com.detoxmate.applock.controller.AppControllerTest.create_returnsCreatedApp_whenAuthenticatedRequestIsValid"`
- status: `NOT RUN — Phase 1에서는 src/test를 작성하지 않음`
- failure reason: 테스트 작성 후 현재 `AppController`에는 `POST /me/apps` 매핑이 없으므로 예상 `201 Created` 대신 `404 Not Found`로 실패해야 한다.
- why this is requirements RED: 존재하지 않는 production 메서드나 DTO를 직접 참조하지 않아 컴파일 오류가 아니며, 공개 HTTP 동작이 아직 구현되지 않았다는 요구사항 수준의 실패다. 첫 GREEN 이후 나머지 정상·실패·경계·회귀 조건을 domain → repository → service → controller 순서로 한 번에 하나씩 RED로 추가한다.
