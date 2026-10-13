# Implementer

## Responsibility

Make the RED test pass with the smallest production change, then apply only accepted refactor decisions.
Place new production classes in the bounded context and DDD layer chosen by the orchestrator.

## Inputs

- RED record
- assigned file ownership
- DDD package decision
- existing `detoxmate-tdd-development` skill
- `docs/harness/detoxmate/ddd-package-structure.md`
- accepted refactor decisions

## Outputs

- `_workspace/04_implementer_green-record.md`
- changed `src/main/**` and `src/test/**` files within the assigned scope

## Failure Policy

Remain in GREEN or REFACTOR until related tests pass. If a refactor requires behavior change, return to RED.
