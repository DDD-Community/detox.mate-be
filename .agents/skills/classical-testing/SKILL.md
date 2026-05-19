---
name: classical-testing
description: Write or review behavior-first tests using the classical/classist/Chicago style. Use when Codex needs to add, modify, or critique tests while avoiding implementation-coupled mocks, especially for unit tests around domain logic, integration tests around real module boundaries, mock-heavy test migrations, and requests mentioning "고전파 테스트", "classical testing", "classist", "Chicago TDD", behavior-first tests, or boundary-only mocking.
---

# Classical Testing

Prefer tests that protect behavior from refactors.

Use this skill when writing new tests, repairing brittle tests, or reviewing whether a test is coupled to internals. Keep the skill body short and load the full rulebook from [references/testing-rules.md](references/testing-rules.md) when you need the detailed policy, red flags, or migration guidance.

## Core Stance

- Test behavior, not implementation details.
- Mock only at system boundaries such as DB, filesystem, clocks, randomness, network, and third-party APIs.
- Keep internal collaborators real whenever practical.
- Prefer assertions on returned values, persisted state, emitted externally observable effects, and user-visible outcomes.
- Treat `toHaveBeenCalledWith(...)`, `verify(...)`, and spy-count assertions as secondary evidence, not the main proof.

## Repo Test Conventions

When editing tests in `detox.mate-be`, preserve the conventions that appear across test layers:

- Use Korean `@DisplayName` values that describe the observable behavior or domain rule.
- Keep test method names in English and behavior-oriented, commonly `<methodUnderTest>_<expectedBehavior>` or `<methodUnderTest>_<condition>_<expectedBehavior>`.
- Structure non-trivial tests with `// given`, `// when`, and `// then`; use `// when & then` when the action and assertion are naturally one expression.
- Do not treat layer-specific details such as real repositories, fixed clocks, or database side-effect assertions as repo-wide conventions unless the surrounding test layer already uses them.

## Workflow

### 1. Find the real behavior

- State the behavior in one sentence before writing the test.
- If you cannot state the behavior clearly, do not write the test yet.
- If the requested test only protects framework wiring, plain CRUD, config, or code about to be deleted, push back and explain why.

### 2. Choose the right layer

- Use unit tests for pure logic, state transitions, calculations, parsers, validators, and domain entities.
- Use integration tests for modules that need a real database, queue, or boundary fake.
- Use E2E tests for a critical user journey, not for every branch.
- If logic needs a database only because it lives in a service around a plain row, extract a domain entity and test it in memory.

### 3. Set the boundary

- Mock or fake only the parts that cross a process or system boundary.
- Prefer HTTP-level fakes such as `wiremock`, `msw`, or `nock` over internal collaborator mocks.
- Prefer temp directories or real ephemeral files over mocked filesystem APIs.
- If you feel pressure to mock your own service or module, the unit boundary is probably wrong.

### 4. Write the test from the outside in

- Follow TDD: write the failing test first, then implement the minimum code to pass.
- Name the test by observable behavior, not by a method call or implementation step.
- Prefer whole-object or outcome-level assertions over field-by-field noise.
- Keep one behavior per test. Split the test when the name needs "and".

### 5. Review for brittleness

Reject or rewrite the test if any of these are true:

- It mainly proves a mock interaction.
- It reaches into private or `_internal` APIs.
- It would fail after a pure refactor that preserves behavior.
- It snapshots nondeterministic output.
- It uses `sleep`, retries, or inflated timeouts to hide flakiness.

## Migration Rule

Do not rewrite the whole codebase for sport.

- Apply the policy fully to all new tests.
- When touching an existing test file, move mocks outward toward the true system boundary.
- Prioritize files dominated by interaction assertions.
- Replace nondeterministic snapshots with structural assertions or delete them.
- Introduce a real database or container-backed integration pattern in one high-risk domain first, then expand.

## Property-Based Testing

Switch to property-based testing when the same function is accumulating many example cases around one invariant.

- Good fits: parsers, encoders, validators, sorters, state machines, and normalization logic.
- Rule of thumb: if you are writing the fourth example for the same invariant, consider a property instead.

## How to Respond

When using this skill in a real task:

1. Restate the protected behavior.
2. Pick the thinnest useful test layer.
3. Identify which dependencies are true boundaries.
4. Write or revise the test so the main assertion is behavioral.
5. Call out any remaining brittleness or missing coverage.

## Reference

- Read [references/testing-rules.md](references/testing-rules.md) for the full rulebook, naming template, mock policy, flaky-test handling, migration strategy, and PR red flags.
