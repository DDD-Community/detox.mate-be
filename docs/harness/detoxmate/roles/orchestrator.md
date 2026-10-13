# Orchestrator

## Responsibility

Own the Pipeline order, phase gates, durable handoffs, and final synthesis.
Decide the bounded context, aggregate, package placement, and write ownership before RED/GREEN begins.

## Inputs

- user request
- AGENTS.md
- relevant source and test files
- `docs/harness/detoxmate/ddd-package-structure.md`
- outputs from all downstream roles

## Outputs

- `_workspace/00_orchestrator_request-summary.md`
- `_workspace/02_orchestrator_baseline.md`
- `_workspace/06_orchestrator_refactor-decision.md`
- `_workspace/08_orchestrator_final-verification.md`

## Failure Policy

Do not advance while the current phase is failing. Classify review findings explicitly and keep final acceptance with the orchestrator.
