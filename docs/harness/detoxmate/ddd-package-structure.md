# Detoxmate DDD Package Structure

## Goal

Detoxmate 코드는 기술 계층보다 비즈니스 도메인 경계를 먼저 드러내야 한다. 새 기능을 추가하거나 큰 변경을 설계할 때는 먼저 어떤 Bounded Context와 Aggregate가 책임지는지 결정하고, 그 다음 패키지 위치를 정한다.

좋은 변경은 다음처럼 읽혀야 한다.

```java
GroupChallenge challenge = groupChallengeRepository.getById(challengeId);
challenge.start(now);
groupChallengeRepository.save(challenge);
```

반대로 다음처럼 상태 값만 외부에서 조작하는 흐름은 피한다.

```java
challenge.setStatus(GroupChallengeStatus.IN_PROGRESS);
challenge.setStartedAt(now);
groupChallengeRepository.save(challenge);
```

## Bounded Context First

최상위 패키지는 `com.detoxmate.{domain}` 형태의 업무 단위가 우선이다.

현재 Detoxmate의 대표 도메인 경계는 다음과 같이 본다.

- `user`: 사용자, 소셜 로그인 사용자, 프로필, 탈퇴
- `auth`: 토큰, 인증 세션, 현재 사용자 해석
- `group`: 그룹, 그룹 멤버, 그룹 챌린지, 그룹 활동 캘린더
- `challengerecord`: 챌린지 인증 기록
- `challengerecordstatuscount`: 챌린지 인증 상태 집계
- `activityrecord`: 앱 사용 기록과 목표 시간
- `firstscreentime`: 최초 스크린타임 기록
- `feed`: 홈/그룹 피드 조회와 피드 상세
- `comment`: 댓글
- `reaction`: 리액션
- `poke`: 찌르기
- `notification`: 알림 템플릿, 발송, 이력, FCM 토큰
- `screentimeocr`: 스크린타임 OCR 오류 신고와 관리자 처리
- `upload`: 업로드 목적, presigned URL, 저장소 연동
- `admin`, `dev`: 운영자/개발 편의 경계
- `common`, `config`: 도메인이 아닌 전역 기술 기반

새 최상위 패키지를 만들기 전에는 기존 도메인 경계에 속하는지 먼저 확인한다. `controller`, `service`, `repository`, `dto`, `entity` 같은 기술 계층명을 최상위 패키지로 만들지 않는다.

## Preferred Structure For New Contexts

새 bounded context를 만들거나 기존 도메인의 큰 기능을 분리할 때는 다음 구조를 기본으로 한다.

```text
com.detoxmate.{domain}
├── domain
│   ├── {AggregateRoot}.java
│   ├── {ValueObject}.java
│   ├── {DomainEvent}.java
│   ├── {DomainPolicy}.java
│   └── {AggregateRoot}Repository.java
├── application
│   ├── {UseCase}Service.java
│   ├── {UseCase}Command.java
│   └── {UseCase}Result.java
├── presentation
│   ├── {Domain}Controller.java
│   ├── {UseCase}Request.java
│   └── {UseCase}Response.java
└── infrastructure
    ├── persistence
    │   ├── Jpa{AggregateRoot}Repository.java
    │   └── {AggregateRoot}RepositoryAdapter.java
    ├── client
    └── messaging
```

Small contexts may omit empty directories. Do not create `port`, `adapter`, `factory`, `facade`, or mapper layers before they remove real complexity.

## Legacy-Compatible Mapping

The repository already has many domain-first packages with layer names inside them, such as `group/controller`, `group/service`, `group/repository`, and `group/dto`. Do not perform broad package moves just to satisfy the harness.

When changing an existing area:

- narrow bug fix: follow the existing package style and improve domain behavior in place
- new business rule inside an existing aggregate: prefer adding behavior to `domain` and tests near the current package
- new use case with meaningful orchestration: put the flow in the current service package or a new `application` package if the surrounding area is already being separated
- new external technology implementation: keep it outside `domain`; use `infrastructure`, existing `repository`, existing `util`, or a clearly named adapter package
- new API request/response DTO: keep it outside `domain`; use existing `dto` or `presentation`

If a change introduces `application`, `presentation`, or `infrastructure` in an existing domain, move only the files needed for the current behavior and update imports/tests together.

## Dependency Direction

The intended dependency direction is:

```text
presentation -> application -> domain
infrastructure -> domain / application
```

Domain must not depend on:

- Spring MVC or HTTP types
- Controller request/response DTOs
- external clients such as Firebase, Kakao, Apple, S3, Discord, or WebClient
- `EntityManager`, Redis, Kafka, scheduler, or lock implementations
- application services

Pragmatic exception: Detoxmate may keep JPA annotations on domain entities. JPA convenience is acceptable when it does not move business rules out of the domain model or expose arbitrary setters.

## Aggregate Rules

Aggregate boundaries are based on transactional consistency, not table count.

- modify child entities through the Aggregate Root
- avoid opening public setters for business state
- express state changes as business methods such as `join`, `withdraw`, `certify`, `approve`, `reject`, `poke`, `markAsRead`
- prefer ID references between different Aggregates unless object references are needed for an invariant inside one transaction
- avoid one transaction that changes many unrelated Aggregates unless the consistency requirement is explicit

Examples for Detoxmate:

- `Group`, `GroupMember`, and `GroupChallenge` can be separate Aggregates when their lifecycles and consistency rules differ
- `ChallengeRecord` owns certification result transitions
- `NotificationHistory` owns read/history state, while FCM sending is infrastructure/application orchestration
- `ScreenTimeOcrErrorReport` owns report status changes, while Discord notification is outside the domain object

## Application Service Rules

Application services execute use cases.

They may:

- load Aggregates and collaborators
- call domain methods
- coordinate multiple Aggregates
- call external ports or infrastructure clients through interfaces
- publish events
- control transactions
- return result DTOs or application results

They should not:

- encode core business state transitions that can live on an Entity or Value Object
- expose Entity objects directly to controllers
- depend on controller request/response classes
- become a single service that owns every operation in a domain

## DTO And Query Rules

Controller DTOs and API response DTOs stay outside `domain`.

Complex reads do not need to hydrate full Aggregates when no domain behavior is executed. Use query services, query repositories, projections, or response DTO reads when the use case is read-only.

Command/write paths should keep domain behavior explicit. Query/read paths may optimize for API shape and database efficiency.

## Common Package Rule

Put code in `common` only when it is truly cross-domain and technical.

Good `common` candidates:

- global exception handling
- validation annotations
- request logging
- access token extraction
- shared error response envelope

Bad `common` candidates:

- domain statuses
- domain exceptions
- business validators
- DTOs that only one domain uses
- repositories or services for one business concept

## Package Decision Checklist

Before adding or moving a production class, answer these in `_workspace/00_orchestrator_request-summary.md` or the GREEN record:

1. Which Detoxmate bounded context owns this behavior?
2. Which Aggregate owns the invariant or state transition?
3. Is this a command/write path, query/read path, API boundary, or infrastructure implementation?
4. Which package should own the new class?
5. Does the dependency direction still point toward `domain`?
6. Is this package change necessary for the current requirement, or is it a broad cleanup?

If the answer is unclear, prefer the smallest change inside the existing domain package and record the uncertainty instead of inventing a new hierarchy.
