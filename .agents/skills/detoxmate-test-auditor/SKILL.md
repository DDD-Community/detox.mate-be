---
name: detoxmate-test-auditor
description: GREEN과 리팩터링 이후 Detoxmate Spring Boot/JPA 변경의 누락 테스트를 감사한다.
---

# Detoxmate Test Auditor

## When to Use

- GREEN 구현과 리뷰 기반 리팩터링 이후 테스트 보강이 필요한지 판단할 때 사용한다.
- 회귀, 경계값, 예외, 권한, 트랜잭션, JPA 제약조건, 외부 연동 실패 테스트 누락을 확인할 때 사용한다.
- 테스트 추가가 필요하면 새 RED-GREEN 사이클로 되돌릴 근거를 만들 때 사용한다.

## Required Inputs

- 원래 요구사항과 acceptance criteria
- 현재 git diff
- 실행된 테스트 명령과 결과
- 기존 테스트 파일과 새 테스트 파일
- 관련 도메인, 서비스, Repository, Controller 흐름
- Phase 0의 기존 계약 근거, 호출자/소비자, 문서/정책, git 이력과 미확인 사항

## Workflow

1. acceptance criteria 및 Phase 0의 보존·변경 계약을 실제 테스트와 대조한다. 기존 테스트 assertion의 삭제·완화가 계약 근거 없이 이뤄졌는지 확인한다.
2. 테스트가 관찰 가능한 행위와 상태를 검증하는지 확인한다. API 응답 변경은 실제 HTTP JSON에서 기존 필드의 존재와 값의 의미·null/누락 조건이 검증되는지 확인한다.
3. 다음 누락 후보를 확인한다.
   - 도메인 불변식
   - 경계값
   - 중복 요청
   - 권한
   - 예외 응답
   - 트랜잭션 롤백
   - JPA 제약조건과 연관관계
   - 외부 연동 실패
   - 기존 기능 회귀
   - 호출자/소비자가 의존하는 기존 응답 계약
4. 누락 테스트를 `blocking`, `recommended`, `deferred`로 분류한다.
5. 제거 대상이 사용자의 명시적 요청인지, 호출자 영향과 활성 제품 정책을 조사·기록했는지 확인한다. 제거 의도 또는 중대한 영향이 불명확하거나 활성 정책과 충돌하면 `Needs Product Decision`으로, 근거 없는 계약 제거는 `blocking`으로 표시하고 완료로 판정하지 않는다. `blocking` 누락이 있으면 Phase 3 RED로 돌아가야 한다고 명시한다.

## Outputs

- `_workspace/07_test-auditor_test-audit.md`

## Output Format

```markdown
# Test Audit

## Coverage Map
| Acceptance criterion | Existing or new test | Status |
| --- | --- | --- |

## Missing Tests
| Priority | Case | Why it matters | Suggested test |
| --- | --- | --- | --- |

## Contract Check
- preserved and changed behavior evidence:
- existing assertions removed or weakened:
- actual HTTP JSON field presence and meaning:
- unknown or Needs Product Decision:

## Decision
- pass | return-to-red
- reason:
```

## Validation

- 소스 파일과 테스트 파일을 함께 읽고 경계 불일치를 찾는다.
- 단순 존재 여부보다 acceptance criteria와 테스트 assertion의 대응 관계를 검증한다.
- 테스트 통과만으로 기존 계약 보존을 추정하지 않는다.
- `blocking` 누락은 최종 완료 전에 반드시 테스트로 보강한다.
