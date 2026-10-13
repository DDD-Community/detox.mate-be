---
name: detoxmate-tdd-orchestrator
description: Detoxmate Spring Boot/JPA 기능 개발을 Pipeline과 GREEN 이후 Producer-Reviewer 루프로 조정한다.
---

# Detoxmate TDD Orchestrator

## When to Use

- Detoxmate Spring Boot/JPA 저장소에서 기능 추가, 버그 수정, API 변경을 여러 역할로 분리해 진행할 때 사용한다.
- Classicist TDD의 RED-GREEN-REFACTOR 순서와 단계별 테스트 기록이 필요한 작업에 사용한다.
- 단일 에이전트가 충분한 작은 작업은 `$detoxmate-tdd-development`만 사용해도 된다.

## Required Inputs

- 사용자 요구사항과 완료 기준
- 관련 패키지, 엔티티, 서비스, 컨트롤러, Repository, 테스트 위치
- 관련 Bounded Context, Aggregate, DDD 패키지 위치
- 실행 가능한 관련 테스트 명령
- 변경 가능한 파일 범위와 병렬 작업 여부

## Architecture

외곽 구조는 `Pipeline`이다. 각 단계는 이전 단계 산출물을 입력으로 받으며, 실패 상태에서는 다음 단계로 넘어가지 않는다.

구현과 리팩터링 사이에는 `Producer-Reviewer` 구조를 적용한다.

- `implementer`가 GREEN 산출물을 만든다.
- `refactor-reviewer`가 기존 `.agents/skills/detoxmate-code-review/SKILL.md`를 적용해 읽기 전용 리뷰를 수행한다.
- `orchestrator`가 Finding을 `ACCEPTED`, `REJECTED`, `DEFERRED`로 분류하고 리팩터링 범위를 결정한다.

## Workflow

### Phase 0. Intake

- 요청을 기능 단위로 좁힌다.
- 관련 기존 코드와 테스트를 찾는다.
- `docs/harness/detoxmate/ddd-package-structure.md` 기준으로 Bounded Context, Aggregate, 새 클래스 패키지 위치를 정한다.
- 변경 파일 소유권을 선언한다.
- output: `_workspace/00_orchestrator_request-summary.md`

### Phase 1. Test Design

- `test-designer`가 사용자 요구사항을 acceptance criteria와 테스트 후보로 쪼갠다.
- 정상, 실패, 경계, 회귀 조건을 분리한다.
- 기존 테스트 실행 명령을 먼저 정한다.
- output: `_workspace/01_test-designer_acceptance-criteria.md`

### Phase 2. Baseline Green

- 관련 기존 테스트를 실행한다.
- 기존 실패는 새 요구사항의 RED로 간주하지 않는다.
- 실패하면 원인과 재현 명령을 기록하고 진행을 멈춘다.
- output: `_workspace/02_orchestrator_baseline.md`

### Phase 3. RED

- 가장 작은 관찰 가능한 행위 테스트를 먼저 작성한다.
- `src/main`은 수정하지 않는다.
- RED 인정 조건은 컴파일 성공, 테스트 실패, 실패 원인이 요구사항 미구현인 경우다.
- output: `_workspace/03_test-designer_red-record.md`

### Phase 4. GREEN

- `implementer`가 `$detoxmate-tdd-development`의 GREEN 규칙을 따라 최소 구현만 작성한다.
- 새 production class는 Phase 0에서 정한 DDD 패키지 위치에 둔다.
- 신규 테스트, 관련 테스트 클래스, 관련 도메인 테스트 순서로 실행한다.
- 실패하면 같은 단계에서 수정하며 다음 단계로 넘어가지 않는다.
- output: `_workspace/04_implementer_green-record.md`

### Phase 5. Refactor Review

- `refactor-reviewer`는 코드를 수정하지 않고 현재 git diff만 검토한다.
- 기존 `.agents/skills/detoxmate-code-review/SKILL.md`를 재사용한다.
- DDD 패키지 구조, 의존성 방향, SOLID, Spring/JPA/Hibernate, 트랜잭션, 영속성 컨텍스트, 연관관계, N+1, 테스트 품질을 확인한다.
- output: `_workspace/05_refactor-reviewer_review.md`

### Phase 6. Refactor

- `orchestrator`가 리뷰 Finding을 `ACCEPTED`, `REJECTED`, `DEFERRED`로 분류한다.
- `implementer`는 ACCEPTED Finding만 작은 단위로 반영한다.
- 각 리팩터링 후 관련 테스트를 실행하고 결과를 기록한다.
- 동작 변경이 필요하면 Phase 3으로 돌아간다.
- output: `_workspace/06_orchestrator_refactor-decision.md`

### Phase 7. Test Audit

- `test-auditor`가 회귀, 경계값, 예외, 권한, 트랜잭션 롤백, JPA 제약조건, 외부 연동 실패 테스트 누락을 확인한다.
- 누락 테스트가 있으면 Phase 3으로 돌아가 RED-GREEN으로 추가한다.
- output: `_workspace/07_test-auditor_test-audit.md`

### Phase 8. Final Verification

- 전체 테스트와 빌드를 실행한다.
- `git diff --check`, `git status`, `git diff`를 확인한다.
- output: `_workspace/08_orchestrator_final-verification.md`

## Handoff Rules

- 같은 파일을 여러 역할이 동시에 수정하지 않는다.
- 패키지 이동이나 새 패키지 추가는 Phase 0의 DDD 패키지 결정에 포함되어야 한다.
- 병렬 작업은 읽기 전용 조사나 서로 다른 파일 소유권이 명확한 경우에만 허용한다.
- 테스트가 실패한 상태에서는 다음 Pipeline 단계로 넘어가지 않는다.
- durable handoff는 `_workspace/{phase}_{role}_{artifact}.md` 형식으로 남긴다.
- subagent가 직접 파일을 쓰지 않는 운영에서는 parent orchestrator가 subagent 결과를 같은 형식으로 저장한다.

## Outputs

- 요구사항과 acceptance criteria
- RED 실패 기록과 명령
- GREEN 구현 기록과 명령
- 코드 리뷰 Finding과 처리 분류
- 리팩터링 기록
- 테스트 감사 결과
- 최종 검증 결과와 변경 파일 목록

## Validation

완료 전에 다음 명령을 실행한다.

```bash
./gradlew test
./gradlew clean build
git diff --check
git status
git diff
```

Gradle 프로젝트 파일이 없는 하네스 저장소에서는 구조 검증으로 대체하고, 실제 Spring Boot 대상 저장소에서는 위 명령을 필수로 실행한다.

## References

- `.agents/skills/detoxmate-tdd-development/SKILL.md`
- `.agents/skills/detoxmate-code-review/SKILL.md`
- `docs/harness/detoxmate/ddd-package-structure.md`
- `docs/harness/detoxmate/team-spec.md`
