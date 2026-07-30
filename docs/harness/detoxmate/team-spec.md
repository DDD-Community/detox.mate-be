# Detoxmate TDD Harness Team Spec

## Domain Summary

Detoxmate는 Spring Boot와 JPA/Hibernate 기반 백엔드이다. 이 하네스의 목적은 기능 추가와 버그 수정을 Classicist TDD로 수행하고, 새 코드가 기술 계층보다 Detoxmate 도메인 경계와 Aggregate를 먼저 드러내도록 패키지 위치를 결정하며, GREEN 이후 DDD/SOLID/JPA 관점 리뷰를 통해 리팩터링한 뒤 테스트 누락을 감사하는 재사용 가능한 개발 흐름을 제공하는 것이다.

현재 저장소에는 기존 `detoxmate-tdd-development` 스킬, `detoxmate-code-review` 스킬, Codex agent 설정, TDD 훅 파일이 있다. 새 하네스는 기존 코드 리뷰 스킬을 복제하지 않고 참조한다.

## Architecture Pattern

- outer pattern: Pipeline
- local pattern: Producer-Reviewer between `implementer` and `refactor-reviewer`
- synthesis owner: `orchestrator`
- delegation depth: one downstream layer only

Pipeline을 기본으로 선택한 이유는 RED, GREEN, REVIEW, REFACTOR, AUDIT, VERIFY가 순차 의존 관계를 갖기 때문이다. 구현과 리팩터링 사이에 Producer-Reviewer를 둔 이유는 GREEN 상태의 변경분을 별도 품질 기준으로 검토해야 하지만, 최종 반영 여부는 오케스트레이터가 요구사항과 비용을 보고 결정해야 하기 때문이다.

## Roles

| Role | Responsibility | Skill or brief | Writes |
| --- | --- | --- | --- |
| orchestrator | 단계 순서, 통과 조건, 파일 소유권, 최종 합성을 관리한다. | `.agents/skills/detoxmate-tdd-orchestrator/SKILL.md` | `_workspace/00_*`, `_workspace/02_*`, `_workspace/06_*`, `_workspace/08_*` |
| test-designer | acceptance criteria와 RED 테스트를 먼저 정의한다. | `.agents/skills/detoxmate-test-designer/SKILL.md` | `src/test/**`, `_workspace/01_*`, `_workspace/03_*` |
| implementer | RED 테스트를 통과시키는 최소 구현과 승인된 리팩터링만 수행한다. | `.agents/skills/detoxmate-tdd-development/SKILL.md` | 지정된 `src/main/**`, `src/test/**`, `_workspace/04_*` |
| refactor-reviewer | GREEN diff를 읽기 전용으로 리뷰한다. | `.agents/skills/detoxmate-code-review/SKILL.md` | `_workspace/05_*` only, or parent-saved summary |
| test-auditor | 리팩터링 후 누락 테스트와 회귀 위험을 감사한다. | `.agents/skills/detoxmate-test-auditor/SKILL.md` | `_workspace/07_*` only, or parent-saved summary |

## Phase Order

### Phase 0. Intake

- input sources: user request, AGENTS.md, related source and test files
- actions: scope the feature, identify bounded context and aggregate, decide DDD package placement, list candidate files, assign write ownership
- output files: `_workspace/00_orchestrator_request-summary.md`
- completion criteria: requirement scope, domain boundary, package placement, and file ownership are explicit

### Phase 1. Test Design

- input sources: request summary, current tests, related production code
- actions: define acceptance criteria and RED test candidate
- output files: `_workspace/01_test-designer_acceptance-criteria.md`
- completion criteria: normal, failure, boundary, regression criteria are separated

### Phase 2. Baseline Green

- input sources: related existing tests
- actions: run relevant tests before new RED
- output files: `_workspace/02_orchestrator_baseline.md`
- completion criteria: related baseline is green, or pre-existing failures are reported and the feature work stops

### Phase 3. RED

- input sources: acceptance criteria
- actions: write one smallest failing test before touching `src/main`
- output files: `_workspace/03_test-designer_red-record.md`
- completion criteria: test compiles, fails for missing behavior, and command output is recorded

### Phase 4. GREEN

- input sources: RED test and record
- actions: implement the smallest production change that passes the RED test in the agreed domain/package boundary
- output files: `_workspace/04_implementer_green-record.md`
- completion criteria: new test and related tests pass

### Phase 5. Refactor Review

- input sources: user request, acceptance criteria, current git diff, GREEN record
- actions: apply existing `detoxmate-code-review` skill in read-only mode
- output files: `_workspace/05_refactor-reviewer_review.md`
- completion criteria: findings are concrete, evidenced, scoped to current diff, and include DDD package/dependency risks when relevant

### Phase 6. Refactor

- input sources: review findings and GREEN diff
- actions: classify findings as `ACCEPTED`, `REJECTED`, or `DEFERRED`; apply accepted refactors only
- output files: `_workspace/06_orchestrator_refactor-decision.md`
- completion criteria: related tests pass after every refactor step

### Phase 7. Test Audit

- input sources: acceptance criteria, changed tests, changed production code, verification records
- actions: check for missing regression, boundary, exception, transaction, and JPA tests
- output files: `_workspace/07_test-auditor_test-audit.md`
- completion criteria: no blocking missing tests remain; otherwise return to Phase 3

### Phase 8. Final Verification

- input sources: final diff and all handoff files
- actions: run full tests, final build, diff checks
- output files: `_workspace/08_orchestrator_final-verification.md`
- completion criteria: full test and build commands pass, final response includes changed files and verification results

## Handoff Files

| From | To | File | Purpose |
| --- | --- | --- | --- |
| orchestrator | test-designer | `_workspace/00_orchestrator_request-summary.md` | stable request scope and file ownership |
| test-designer | orchestrator, implementer | `_workspace/01_test-designer_acceptance-criteria.md` | acceptance criteria and test plan |
| test-designer | implementer | `_workspace/03_test-designer_red-record.md` | proof that RED is valid before production changes |
| implementer | refactor-reviewer | `_workspace/04_implementer_green-record.md` | GREEN state and executed commands |
| refactor-reviewer | orchestrator | `_workspace/05_refactor-reviewer_review.md` | read-only findings using the existing review skill |
| orchestrator | implementer | `_workspace/06_orchestrator_refactor-decision.md` | accepted, rejected, and deferred review actions |
| test-auditor | orchestrator | `_workspace/07_test-auditor_test-audit.md` | missing test audit and return-to-RED decision |
| orchestrator | user | `_workspace/08_orchestrator_final-verification.md` | final verification evidence |

## Write Ownership

- `test-designer` may write only assigned test files during RED.
- `implementer` may write assigned production and test files only after RED is recorded.
- New packages or package moves must be included in the Phase 0 DDD package decision.
- `refactor-reviewer` does not modify files.
- `test-auditor` does not modify production code; it reports missing tests for a new RED cycle.
- `orchestrator` owns `_workspace/` synthesis files and final reporting.
- Parallel writes to the same file, package, migration, fixture, or generated artifact are forbidden.

## DDD Package Policy

Use `docs/harness/detoxmate/ddd-package-structure.md` as the package placement contract for development and review.

- Top-level packages should be Detoxmate business contexts such as `group`, `activityrecord`, `notification`, `screentimeocr`, `upload`, `user`, `auth`, `feed`, `comment`, `reaction`, and `poke`.
- Do not create top-level packages named only by technical layers, such as `controller`, `service`, `repository`, `dto`, or `entity`.
- For new bounded contexts or large feature separations, prefer `{domain}/domain`, `{domain}/application`, `{domain}/presentation`, and `{domain}/infrastructure`.
- For narrow changes in existing packages, avoid broad migration and improve the current domain package in place.
- Business rules and state transitions should live in Domain objects where natural; Application services coordinate use cases, transactions, repositories, and external systems.
- Domain must not depend on Controller DTOs, HTTP types, external clients, messaging/cache/storage implementations, or application services.
- Complex read-only use cases may use query services/repositories and DTO projections without forcing Aggregate hydration.

## Failure Policy

- Baseline failure: stop feature work and report the pre-existing failure separately.
- RED invalid: rewrite the test before production changes.
- GREEN failure: remain in Phase 4 until the targeted and related tests pass.
- Review disagreement: orchestrator records the decision as `REJECTED` or `DEFERRED` with a reason.
- Refactor failure: revert only the agent's own refactor change or return to RED if behavior changed.
- Test audit blocking gap: return to Phase 3 and add the missing test through RED-GREEN.
- Final build failure: do not report completion; record command, failure, and next fix target.

## Validation Commands

Related test:

```bash
./gradlew test --tests "<test class or method>"
```

Full test:

```bash
./gradlew test
```

Final build:

```bash
./gradlew clean build
```

Diff checks:

```bash
git diff --check
git status
git diff
```

## Scenario Tests

### Normal Flow

- request: add a small use case to an existing service
- expected phase outputs: request summary, acceptance criteria, RED record, GREEN record, review, refactor decision, test audit, final verification
- expected package output: request summary records the bounded context, aggregate, target package, and dependency direction
- expected final output: changed files, RED/GREEN evidence, review decisions, test and build results

### Failure Flow

- failure point: related baseline test fails before a new RED test is written
- expected fallback behavior: stop before modifying `src/main`, record the failing command in `_workspace/02_orchestrator_baseline.md`, ask whether to fix the pre-existing failure or narrow the task
- expected reporting: final response clearly states that new feature work did not proceed because baseline was not green

## Removable Codex Adapter

`.codex/agents/*.toml` files are draft runtime adapters. The portable contract remains this team spec and the repo-local skills. Model settings are intentionally omitted in new drafts so agents inherit the parent Codex configuration unless a measured reason appears later.
