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
- 관련 호출 흐름, 호출자/소비자, 패키지, 엔티티, 서비스, 컨트롤러, Repository, 테스트 위치
- 기존 동작의 근거가 되는 문서/정책과 관련 git 이력
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
- 수정 대상의 실제 호출 흐름과 호출자/소비자, 관련 코드·테스트·문서/정책·git 이력을 확인한다. 관련 근거를 못 찾으면 `미확인`으로 기록하며 불필요한 동작이라고 추정하지 않는다.
- 기존 동작의 이유와 보존할 계약, 의도적으로 바꿀 계약을 근거와 함께 적는다. API 응답은 필드의 존재 여부뿐 아니라 값의 의미·생성 조건·null/누락 조건을 확인한다.
- 사용자가 기존 동작/필드의 제거 대상을 명시하면 호출자 영향과 활성 제품 정책을 조사·기록하고, 아래 보류 조건이 없으면 의도적 계약 변경으로 진행한다. 제거 의도 또는 중대한 영향이 불명확하거나 활성 정책과 충돌하면 `Needs Product Decision`으로 표시하고 해당 계약을 바꾸는 RED/GREEN은 결정 전까지 진행하지 않는다. `필드 3개만` 같은 문구만으로 기존 필드를 제거하지 않는다.
- `docs/harness/detoxmate/ddd-package-structure.md` 기준으로 Bounded Context, Aggregate, 새 클래스 패키지 위치를 정한다.
- 변경 파일 소유권을 선언한다.
- 위 계약 근거와 미확인 사항을 `_workspace/00_orchestrator_request-summary.md`에 기록한다.

### Phase 1. Test Design

- `test-designer`가 사용자 요구사항을 acceptance criteria와 테스트 후보로 쪼갠다.
- 정상, 실패, 경계, 회귀 조건을 분리하고 보존할 기존 계약을 명시한다. 기존 assertion의 삭제·약화가 필요한 후보는 명시적 제거 요청과 영향 조사에 근거하는지 따로 검토한다.
- 기존 테스트 실행 명령을 먼저 정한다.
- output: `_workspace/01_test-designer_acceptance-criteria.md`

### Phase 2. Baseline Green

- 관련 기존 테스트를 실행한다.
- API 계약 변경이면 가능한 기존 HTTP 테스트의 실제 JSON 결과를 기준선으로 확인하고 필드 존재·값의 의미를 기록한다.
- 기존 실패는 새 요구사항의 RED로 간주하지 않는다.
- 실패하면 원인과 재현 명령을 기록하고 진행을 멈춘다.
- output: `_workspace/02_orchestrator_baseline.md`

### Phase 3. RED

- 가장 작은 관찰 가능한 행위 테스트를 먼저 작성한다.
- 새 요구사항과 기존 계약을 함께 고정한다. 응답 변경은 DTO 내부값만 보지 말고 실제 HTTP JSON의 기존 필드와 새 필드를 검증한다.
- `src/main`은 수정하지 않는다.
- RED 인정 조건은 컴파일 성공, 테스트 실패, 실패 원인이 요구사항 미구현인 경우다.
- output: `_workspace/03_test-designer_red-record.md`

### Phase 4. GREEN

- `implementer`가 `$detoxmate-tdd-development`의 GREEN 규칙을 따라 최소 구현만 작성한다.
- 기존 프로젝트 패턴/도우미 → JDK·플랫폼 기능 → 설치된 의존성 → 새 코드 순서로 가장 작은 완결된 변경을 고른다. 필수 검증·오류 처리·보안·테스트는 줄이지 않는다.
- 새 production class는 Phase 0에서 정한 DDD 패키지 위치에 둔다.
- 신규 테스트, 관련 테스트 클래스, 관련 도메인 테스트 순서로 실행한다.
- 실패하면 같은 단계에서 수정하며 다음 단계로 넘어가지 않는다.
- output: `_workspace/04_implementer_green-record.md`

### Phase 5. Refactor Review

- `refactor-reviewer`는 코드를 수정하지 않고 diff와 Phase 0의 기존 호출 흐름·계약 근거를 대조한다.
- 기존 `.agents/skills/detoxmate-code-review/SKILL.md`를 재사용한다.
- 보존할 응답 필드, 수정된 기존 assertion, 실제 HTTP JSON과 DDD 패키지 구조, 의존성 방향, SOLID, Spring/JPA/Hibernate, 트랜잭션, 영속성 컨텍스트, 연관관계, N+1, 테스트 품질을 확인한다.
- 새 추상화·래퍼·인터페이스·설정·계층의 현재 필요성을 확인하고 더 단순한 프로젝트 내 대안이 있으면 제시한다.
- output: `_workspace/05_refactor-reviewer_review.md`

### Phase 6. Refactor

- `orchestrator`가 리뷰 Finding을 `ACCEPTED`, `REJECTED`, `DEFERRED`로 분류한다.
- `implementer`는 ACCEPTED Finding만 작은 단위로 반영한다.
- 각 리팩터링 후 관련 테스트를 실행하고 결과를 기록한다.
- 동작 변경이 필요하면 Phase 3으로 돌아간다.
- output: `_workspace/06_orchestrator_refactor-decision.md`

### Phase 7. Test Audit

- `test-auditor`가 회귀, 경계값, 예외, 권한, 트랜잭션 롤백, JPA 제약조건, 외부 연동 실패 테스트 누락을 확인한다.
- Phase 0의 보존 계약과 수정된 기존 assertion을 대조한다. API 계약 변경은 실제 HTTP JSON에서 필드 존재와 의미가 유지되는지 확인한다.
- 누락 테스트가 있으면 Phase 3으로 돌아가 RED-GREEN으로 추가한다.
- output: `_workspace/07_test-auditor_test-audit.md`

### Phase 8. Final Verification

- 전체 테스트와 빌드를 실행한다.
- 의도하지 않은 계약 삭제와 근거 없는 복잡성이 남지 않았는지 Phase 0 기록과 최종 diff를 대조한다.
- `git diff --check`, `git status`, `git diff`를 확인한다.
- output: `_workspace/08_orchestrator_final-verification.md`

## Handoff Rules

- 같은 파일을 여러 역할이 동시에 수정하지 않는다.
- 패키지 이동이나 새 패키지 추가는 Phase 0의 DDD 패키지 결정에 포함되어야 한다.
- 병렬 작업은 읽기 전용 조사나 서로 다른 파일 소유권이 명확한 경우에만 허용한다.
- 테스트가 실패한 상태에서는 다음 Pipeline 단계로 넘어가지 않는다.
- durable handoff는 `_workspace/{phase}_{role}_{artifact}.md` 형식으로 남긴다.
- 계약 근거, `미확인`, `Needs Product Decision`, 단순화 판단은 새 산출물 없이 해당 단계의 기존 handoff에 기록한다.
- subagent가 직접 파일을 쓰지 않는 운영에서는 parent orchestrator가 subagent 결과를 같은 형식으로 저장한다.

## Outputs

- 요구사항과 acceptance criteria
- 기존 동작의 근거, 보존·변경 계약, 미확인 사항과 제품 결정 필요 항목
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
