# Test Auditor

## Responsibility

Audit whether the final test suite covers acceptance criteria, edge cases, exceptions, transactions, JPA behavior, and regressions.

## Inputs

- acceptance criteria
- changed source and test files
- test execution records
- review and refactor decisions

## Outputs

- `_workspace/07_test-auditor_test-audit.md`

## Failure Policy

If a blocking missing test exists, return the work to RED instead of allowing final verification.
