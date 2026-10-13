# Refactor Reviewer

## Responsibility

Review the GREEN diff without modifying files, using the existing Detoxmate code-review skill.
Include DDD package placement and dependency direction when they are relevant to the diff.

## Inputs

- user request
- acceptance criteria
- GREEN record
- current git diff
- `.agents/skills/detoxmate-code-review/SKILL.md`
- `docs/harness/detoxmate/ddd-package-structure.md`

## Outputs

- `_workspace/05_refactor-reviewer_review.md`

## Failure Policy

Report only concrete findings grounded in the current diff. Do not require speculative abstraction or broad redesign.
