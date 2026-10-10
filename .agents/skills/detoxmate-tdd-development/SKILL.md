---
name: detoxmate-tdd-development
description: Detoxmate Spring Boot/JPA 기능 추가와 버그 수정을 Classicist TDD의 RED-GREEN-REFACTOR 순서로 구현한다. "개발해줘", "구현해줘", "API 추가", "기능 추가", "버그 수정", "TDD로 구현" 요청에서 사용한다.
---

# Detoxmate TDD Development

## 목적

사용자의 요구사항을 현재 Detoxmate 구조에 맞게 분석하고,
실패 테스트부터 작성하여 구현과 리팩터링을 완료한다.

## Phase 0. Discover

다음을 탐색한다.

- 관련 Controller
- Application Service
- Domain Entity와 Value Object
- Repository
- Exception과 ErrorCode
- 기존 테스트
- Transaction 경계
- 외부 연동
- Bounded Context, Aggregate, DDD 패키지 위치
- 수정 대상의 실제 호출 흐름과 호출자/소비자, 관련 문서/정책, git 이력

기존 코드·테스트·문서/정책·git 이력에서 동작의 이유를 확인한다. 확인되지 않은 이유는 `미확인`으로 남기며 삭제 가능하다고 추정하지 않는다. 기존 응답은 필드 존재뿐 아니라 값의 의미·생성 조건·null/누락 조건을 적는다.

코드 근거를 바탕으로 Acceptance Criteria를 정리한다. 사용자가 기존 동작/필드의 제거 대상을 명시하면 호출자 영향과 활성 제품 정책을 조사·기록하고, 아래 보류 조건이 없으면 의도적 변경으로 진행한다. 제거 의도 또는 중대한 영향이 불명확하거나 활성 정책과 충돌하면 `Needs Product Decision`으로 표시하고 결정 전에는 해당 계약을 바꾸는 테스트나 제품 코드를 작성하지 않는다. 응답 필드 수를 제한한 문구만으로 기존 필드를 제거하지 않는다.

Acceptance Criteria는 다음으로 나눈다.

- 정상 동작
- 실패 동작
- 경계 조건
- 기존 동작 보존 조건

새 production class를 추가하거나 패키지를 이동해야 하면 먼저 `docs/harness/detoxmate/ddd-package-structure.md`를 읽고 다음을 정리한다.

- 이 기능을 소유하는 Detoxmate bounded context
- 상태 변경이나 불변식을 책임지는 Aggregate
- command/write, query/read, presentation, infrastructure 중 변경 성격
- 새 클래스의 패키지 위치와 의존성 방향

## Phase 1. Baseline Green

변경 전에 관련 기존 테스트를 실행한다.

API 계약 변경이면 기존 HTTP 테스트의 실제 JSON을 기준선으로 확인한다.

기존 테스트가 실패하면 해당 실패를 새 요구사항의 RED로 간주하지 않는다.

## Phase 2. RED

가장 작은 행위 테스트 하나를 작성한다.

기존 계약을 보존하는 회귀 조건도 함께 고정한다. API 응답 변경은 DTO 내부 상태만이 아니라 실제 HTTP JSON의 필드 존재와 값의 의미를 검증한다. 기존 assertion을 삭제하거나 약화해야 한다면 명시적 제거 요청과 호출자 영향·정책 조사 결과를 먼저 기록한다.

RED 인정 조건:

1. 테스트가 컴파일된다.
2. 테스트가 실패한다.
3. 실패 원인이 요구사항 미구현이다.
4. 제품 코드는 아직 수정되지 않았다.

실패 명령과 실패 원인을 기록한다.

## Phase 3. GREEN

실패 테스트를 통과시키는 최소 구현을 작성한다.

먼저 수정 범위와 기존 패턴을 읽고, 기존 프로젝트 패턴/도우미 → JDK·플랫폼 기능 → 설치된 의존성 → 새 코드 순으로 완결된 해법을 고른다. 몇 줄을 줄이기 위해 새 의존성을 추가하거나 현재 요구가 입증하지 못한 추상화·래퍼·인터페이스·설정·추가 계층을 만들지 않는다. 짧은 diff를 위해 필요한 호출자·테스트·검증·오류 처리·보안 로직을 생략하지 않는다.

금지 사항:

- 불필요한 인터페이스 추가
- 확인되지 않은 확장 포인트 추가
- 요구사항과 무관한 패키지 재구성
- 최상위 `controller`, `service`, `repository`, `dto`, `entity` 패키지 추가
- 새 비즈니스 규칙을 Application Service의 if/else와 setter 호출로만 구현
- Controller Request/Response DTO나 외부 클라이언트를 Domain 객체가 알게 만들기
- 테스트를 구현에 맞게 약화
- 기존 코드를 한꺼번에 정리
- 현재 요구와 무관한 확장성·유연성 구현

구현 원칙:

- 최상위는 `com.detoxmate.{domain}` 형태의 도메인/기능 패키지를 우선한다.
- 신규 bounded context나 큰 기능 분리는 `{domain}/domain`, `{domain}/application`, `{domain}/presentation`, `{domain}/infrastructure` 구조를 우선 검토한다.
- 기존 도메인의 작은 수정은 광범위한 패키지 이동 없이 현재 구조 안에서 도메인 행위를 강화한다.
- 비즈니스 상태 변경은 `setStatus(...)`보다 `certify(...)`, `withdraw(...)`, `approve(...)`, `markAsRead(...)`처럼 의미 있는 도메인 메서드로 표현한다.
- Application Service는 조회, 도메인 메서드 실행, 저장, 외부 연동 조율과 트랜잭션 경계를 담당한다.
- Domain은 HTTP, JSON, Firebase, Kakao, Apple, S3, Discord, Redis, Kafka, 스케줄러, 락 구현을 직접 알지 않는다.

다음 순서로 검증한다.

1. 신규 테스트
2. 관련 테스트 클래스
3. 관련 도메인 테스트

## Phase 4. Independent Review

GREEN 상태에서 `code_reviewer` Subagent를 호출한다.

Subagent에게 다음을 요청한다.

- 현재 git diff 검토
- 실제 호출 흐름 추적
- 기존 계약 근거와 수정된 기존 assertion 대조
- 실제 HTTP JSON의 필드 존재·의미와 새 복잡성의 현재 필요성 검토
- `detoxmate-code-review` Skill 적용
- DDD, SOLID, JPA, 테스트 품질 검토
- 코드는 수정하지 않고 Finding만 반환

각 Finding을 다음으로 분류한다.

- ACCEPTED
- REJECTED
- DEFERRED

## Phase 5. REFACTOR

ACCEPTED Finding을 작은 단위로 적용한다.

각 리팩터링 후 관련 테스트를 실행한다.

리팩터링 중 동작 변경이 필요해지면
새 테스트를 작성하고 RED-GREEN 단계로 돌아간다.

## Phase 6. Test Hardening

다음 테스트가 누락되지 않았는지 확인한다.

- 도메인 불변식
- 경계값
- 중복 요청
- 권한
- 예외 응답
- 트랜잭션 롤백
- JPA 제약조건
- 외부 연동 실패
- 기존 기능 회귀
- 기존 응답 필드의 존재와 의미 및 수정된 기존 assertion

새로운 테스트가 실패하면 새로운 RED-GREEN 사이클로 처리한다.

## Phase 7. Final Verification

다음을 실행한다.

```bash
./gradlew test
./gradlew clean build
git status
git diff --check
git diff
```

전체 검증이 성공하기 전에는 완료했다고 보고하지 않는다.

## Final Report

다음 내용을 보고한다.

1. Acceptance Criteria
2. RED 테스트와 실패 원인
3. GREEN 구현 결과
4. 리뷰 Finding과 처리 결과
5. 리팩터링 내용
6. 추가된 테스트
7. 변경 파일
8. 전체 테스트 및 빌드 결과
9. 남아 있는 위험과 확인 사항

기존 계약의 보존·의도적 변경, 근거가 미확인인 항목, `Needs Product Decision`도 해당 항목에 포함한다.

## References

- `docs/harness/detoxmate/ddd-package-structure.md`
- `.agents/skills/detoxmate-tdd-development/references/classicist-tdd.md`
- `.agents/skills/detoxmate-tdd-development/references/test-strategy.md`
