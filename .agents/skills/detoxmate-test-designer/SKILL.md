---
name: detoxmate-test-designer
description: Detoxmate Spring Boot/JPA 변경 전에 Classicist TDD용 acceptance criteria와 실패 테스트를 설계한다.
---

# Detoxmate Test Designer

## When to Use

- 기능 추가나 버그 수정에서 제품 코드 변경 전에 실패 테스트를 정의해야 할 때 사용한다.
- 요구사항을 정상, 실패, 경계, 회귀 조건으로 분해해야 할 때 사용한다.
- 구현 방식이 아니라 외부에서 관찰 가능한 행위와 상태를 먼저 고정해야 할 때 사용한다.

## Required Inputs

- 사용자 요구사항
- 관련 도메인 객체, 애플리케이션 서비스, Controller, Repository, 기존 테스트
- 수정 대상의 호출자/소비자, 관련 문서/정책, git 이력과 기존 계약 근거
- 관련 Bounded Context, Aggregate, 패키지 구조 기준
- 결정해야 할 비결정적 경계: Clock, UUID, FCM, 외부 API
- 관련 테스트 실행 명령

## Workflow

1. 수정 대상의 실제 호출 흐름과 호출자/소비자, 기존 코드·테스트·문서/정책·git 이력을 읽고 기존 동작의 이유를 기록한다. 근거가 없으면 `미확인`으로 표시한다.
2. acceptance criteria를 정상, 실패, 경계, 회귀 조건으로 분리한다. API 응답은 기존 필드의 존재·값의 의미·생성 조건·null/누락 조건을 보존 조건에 포함한다.
3. 사용자가 기존 동작/필드의 제거 대상을 명시하면 호출자 영향과 활성 제품 정책을 조사·기록하고, 아래 보류 조건이 없으면 RED 계획에 반영한다. 제거 의도 또는 중대한 영향이 불명확하거나 활성 정책과 충돌하면 `Needs Product Decision`을 기록하고, 결정 전에는 해당 계약을 제거하거나 기존 assertion을 약화하는 RED를 작성하지 않는다. `필드 3개만` 같은 문구는 제거 대상을 명시한 요청으로 보지 않는다.
4. 새 production class나 package 이동이 예상되면 `docs/harness/detoxmate/ddd-package-structure.md`를 읽고 테스트 위치를 도메인 경계에 맞춘다.
5. 가장 작은 RED 테스트 하나를 선택한다. 응답 변경은 DTO만이 아니라 실제 HTTP JSON의 기존 필드와 새 동작을 검증한다.
6. 내부 호출 횟수보다 결과 상태, 예외, 응답, 저장 상태를 검증한다.
7. 도메인 규칙은 가능한 한 Spring Context 없이 Domain 테스트로 먼저 고정한다.
8. Repository와 JPA 동작 검증에서는 mock 대신 실제 persistence 테스트를 우선 검토한다.
9. 제품 코드 변경 없이 테스트만 작성하거나, 작성할 테스트 명세를 parent에게 반환한다.
10. RED 실행 명령, 실패 메시지, 실패 원인을 기록한다.

## Outputs

- `_workspace/01_test-designer_acceptance-criteria.md`
- `_workspace/03_test-designer_red-record.md`

## Output Format

```markdown
# Acceptance Criteria

## Normal
- ...

## Failure
- ...

## Boundary
- ...

## Regression
- ...

## Existing Contract Evidence
- call flow and consumers:
- code, test, docs/policy, git history evidence:
- preserved behavior and response field presence/meaning:
- intentional change, explicit removal request, and impact/policy check:
- unknown or Needs Product Decision:
- existing assertions changed or removed, with reason:

# RED Test Plan
- target test file:
- target bounded context:
- target aggregate:
- expected production package:
- test name:
- observable behavior:
- setup data:
- command:

# RED Result
- command:
- status:
- failure reason:
- why this is requirements RED:
```

## Validation

- RED는 컴파일 오류가 아니라 요구사항 미구현으로 실패해야 한다.
- `src/main` 변경 전 RED 기록이 존재해야 한다.
- 테스트 assertion을 구현 세부사항에 과도하게 묶지 않는다.
- 기존 테스트 실패를 새 RED로 둔갑시키지 않는다.
- 기존 assertion을 통과시키려고 계약 검증을 삭제하거나 약화하지 않는다.
- 테스트 패키지는 검증하려는 계층과 도메인 경계를 따라간다.

## References

- `docs/harness/detoxmate/ddd-package-structure.md`
