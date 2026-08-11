# RED Result

- target test file: `src/test/java/com/detoxmate/applock/controller/AppControllerTest.java`
- command: `./gradlew test --tests "com.detoxmate.applock.controller.AppControllerTest"`
- status: `RED` (`BUILD FAILED`, exit code `1`; `compileTestJava`와 `testClasses` 성공; 22 tests, 16 failed)
- GREEN controls: 정상 생성과 `dailyLimitMinutes` 경계값 `0`, `1440`은 통과했다. 존재하지 않는 ID의 GET/PUT/DELETE `404` assertion도 통과했으며, 각 endpoint의 정상 동작 테스트가 별도로 RED여서 미등록 route를 성공으로 오인하지 않는다.
- actual failures:
  - create ownership: expected `403`, actual `201`
  - create validation: null/missing `userId`, blank name, limit `-1`/`1441`은 expected `400`, actual `201`
  - create validation: 101-character name은 expected `400`, actual `500`
  - list: current-user-only 및 empty-list 응답은 expected `200`, actual `500`
  - get own: expected `200`, actual `404`
  - get other-user: 접근 요청의 `404` 후 소유자 조회 control이 expected `200`, actual `404`
  - update own: expected `200`, actual `404`
  - update userId mismatch: expected `403`, actual `404`
  - update other-user: 접근 요청의 `404` 후 소유자 조회 control이 expected `200`, actual `404`
  - delete own: expected `204`, actual `404`
  - delete other-user: 접근 요청의 `404` 후 소유자 조회 control이 expected `200`, actual `404`
- failure reason: 현재 create 흐름은 요청 `userId` 소유권 검증과 request validation이 없고, list/get/update/delete의 공개 HTTP 동작은 아직 구현되지 않았다.
- why this is requirements RED: 모든 fixture App은 현재 GREEN인 `POST /me/apps` API로 만들고, 저장된 실제 `User`, 기존 `JwtTokenProvider`, 실제 HTTP 응답과 후속 조회만 검증한다. 내부 production 메서드나 호출 횟수에 결합하지 않았고 `src/main`은 변경하지 않았다.

## Domain RED — DM-P5-002

- target test file: `src/test/java/com/detoxmate/applock/domain/AppTest.java`
- command: `./gradlew test --tests "com.detoxmate.applock.domain.AppTest"`
- status: `RED` (`BUILD FAILED`, exit code `1`; `compileTestJava`와 `testClasses` 성공; 21 tests, 15 failed)
- GREEN controls: 유효한 aggregate 생성, 유효한 update, 생성·수정의 `dailyLimitMinutes` 경계값 `0`과 `1440`은 통과했다.
- actual failures:
  - null `User` 생성은 expected `IllegalArgumentException`, actual no exception
  - null/empty/whitespace-only/101-character `appDisplayName` 생성은 expected `IllegalArgumentException`, actual no exception
  - null/`-1`/`1441` `dailyLimitMinutes` 생성은 expected `IllegalArgumentException`, actual no exception
  - 잘못된 이름 또는 제한 시간의 update 7개 사례는 expected `IllegalArgumentException`, actual no exception
  - 공통 assertion message: `Expecting code to raise a throwable.`
- failure reason: `App.create`, `App.update`, `AppTimeLimit.create`, `changeDailyLimitMinutes`가 Java-level precondition validation 없이 값을 대입한다.
- why this is requirements RED: Spring/JPA 없이 실제 `User`, `App`, owned `AppTimeLimit`의 공개 행위와 결과 상태만 검증한다. 실패한 update가 기존 이름, 제한 시간, child identity를 보존해야 한다는 aggregate 불변식을 고정하며 `src/main`과 controller 테스트는 변경하지 않았다.
