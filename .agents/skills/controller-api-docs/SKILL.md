---
name: controller-api-docs
description: |
  Controller 테스트 기반으로 REST Docs와 Swagger UI 문서를 추가하거나 갱신합니다.
  Trigger: "swagger ui 문서화", "controller 문서화", "REST Docs 추가", "openapi 추가", "api-docs 추가"
  - 컨트롤러에 Swagger 어노테이션을 붙이지 않고 테스트에서 문서화
  - 지정된 Controller가 없으면 `dev...HEAD` diff 기준으로 변경된 Controller를 대상 선정
  - REST Docs snippet, openapi3.yaml, Swagger UI까지 이어지는 흐름으로 작업
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(git:*), Bash(rg:*), Bash(./gradlew:*), Bash(find:*), Bash(sed:*), Bash(nl:*)
model: sonnet
version: 1.0.0
---

# Controller API Docs

이 스킬은 Controller 기반 API를 `REST Docs -> openapi3.yaml -> Swagger UI` 흐름으로 문서화할 때 사용한다.

## Project Rules

핵심 규칙:

- 컨트롤러에 Swagger 어노테이션을 붙이지 않는다.
- 문서화 코드는 Controller 테스트에 작성한다.
- Swagger UI의 source of truth는 springdoc 자동 스캔이 아니라 `REST Docs` 테스트다.
- 문서 생성 흐름은 `테스트 -> resource.json -> openapi3.yaml -> static resource -> Swagger UI` 이다.
- `build.gradle` 에서는 `openapi3`, `copyOpenapi3Spec`, `bootRun.dependsOn(copyOpenapi3Spec)`, `bootJar.dependsOn(copyOpenapi3Spec)` 흐름을 기준으로 본다.
- `application.yml` 에서는 `springdoc.swagger-ui.url=/openapi3.yaml` 구성을 기준으로 본다.

## Existing Project Pattern

이 저장소의 현재 기준 구현은 아래 파일에 있다.

- `src/test/java/com/detoxmate/user/controller/AuthControllerTest.java`
- `src/test/java/com/detoxmate/user/controller/UserControllerTest.java`
- `src/docs/asciidoc/index.adoc`
- `build.gradle`
- `src/main/resources/application.yml`

의미:

- 테스트가 REST Docs snippet 과 `resource.json` 을 만든다.
- `openapi3` task가 `resource.json` 을 모아 `build/api-spec/openapi3.yaml` 을 만든다.
- `copyOpenapi3Spec` 가 그 파일을 `build/resources/main/static/openapi3.yaml` 로 복사한다.
- Swagger UI는 `/swagger-ui.html` 에서 열리고 `/openapi3.yaml` 을 읽는다.

## Target Selection

### Case 1. 사용자가 특정 Controller 또는 endpoint를 지목한 경우

그 Controller 또는 endpoint만 대상으로 작업한다.

예:

- `AuthController 문서화`
- `UserController에 swagger ui 추가`
- `POST /auth/refresh 문서화`

### Case 2. 사용자가 특정 Controller를 지목하지 않은 경우

`dev` 브랜치와 현재 브랜치 diff를 기준으로 문서화 대상을 찾는다.

기본 순서:

1. `git diff --name-only dev...HEAD` 실행
2. 변경 파일 중 `src/main/java/**/controller/*Controller.java` 우선 탐색
3. 해당 Controller와 대응하는 테스트 파일(`src/test/java/**/controller/*ControllerTest.java`)을 찾는다
4. 변경된 Controller가 없으면, diff에 포함된 request/response DTO 또는 service 변경을 보고 영향을 받는 Controller를 역추적한다
5. 그래도 대상이 없으면 사용자를 멈춰 세우기보다 "diff 기준 문서화 대상 controller 없음"을 짧게 알리고 종료한다

## Implementation Pattern

기존 패턴은 아래 파일을 기준으로 복제한다.

- `src/test/java/com/detoxmate/user/controller/AuthControllerTest.java`
- `src/test/java/com/detoxmate/user/controller/UserControllerTest.java`
- `src/docs/asciidoc/index.adoc`

작업 순서:

1. 성공 응답 테스트를 찾거나 추가한다.
2. 대표적인 실패 응답 테스트도 같이 문서화한다.
3. 테스트에 `@ExtendWith(RestDocumentationExtension.class)` 와 `documentationConfiguration(...)` 이 적용돼 있는지 확인한다.
4. `document("...", ...)` 와 `resource(ResourceSnippetParameters.builder()...)` 를 함께 사용한다.
5. `requestFields(...)`, `responseFields(...)`, `requestHeaders(...)` 가 있으면 REST Docs와 `resource(...)` 양쪽에 같이 넣는다.
6. `requestSchema(...)`, `responseSchema(...)` 를 명시해 component schema 이름을 안정화한다.
7. `src/docs/asciidoc/index.adoc` 에 snippet entry를 추가한다.
8. 필요하면 `build.gradle` 의 문서 파이프라인은 건드리지 말고 기존 흐름에 맞춰 테스트만 추가한다.

## Quality Rules

- 성공 응답뿐 아니라 클라이언트가 구분해서 처리해야 하는 외부 노출 에러 응답은 모두 문서화한다.
- field descriptor와 OpenAPI metadata가 서로 어긋나지 않게 같은 descriptor를 재사용한다.
- nullable string 같은 필드는 `VARIES` 보다 더 구체적인 타입을 우선한다.
- 같은 endpoint의 success/error 문서가 합쳐질 때 operationId가 깨지지 않도록 snippet naming을 일관되게 잡는다.
- 오래된 snippet 때문에 생성물이 오염될 수 있으니 `test.doFirst { delete snippetsDir }` 전제를 깨지 않게 한다.

## Verification

구현 후 아래 순서로 검증한다.

1. `./gradlew test openapi3 copyOpenapi3Spec`
2. 필요하면 `./gradlew asciidoctor`
3. 생성물 확인:
   - `build/generated-snippets/**/resource.json`
   - `build/api-spec/openapi3.yaml`
   - `build/docs/asciidoc/index.html`

확인 포인트:

- 대상 endpoint가 `openapi3.yaml` 에 들어갔는가
- 성공/실패 응답 schema가 모두 반영됐는가
- Swagger UI가 읽는 기준 문서는 `/openapi3.yaml` 인가
- 필요하면 `./gradlew bootRun --args='--spring.profiles.active=local'` 후 `/swagger-ui.html` 과 `/openapi3.yaml` 을 같이 확인한다

## Output Expectations

작업 결과 보고에는 아래를 포함한다.

- 어떤 Controller 또는 endpoint를 문서화했는지
- 어떤 테스트 파일과 Asciidoc entry를 바꿨는지
- 어떤 Gradle 명령으로 검증했는지
- 남은 리스크가 있으면 한 줄로만 명시
