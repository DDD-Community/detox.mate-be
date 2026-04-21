# Testing Rules

Rules for any agent writing or modifying tests in this repo or a similar codebase.

## Core Philosophy

1. Test behavior, not implementation. Pure refactors must not break tests.
2. Mock only at the system boundary. Everything inside is real.
3. Prefer Classist (Chicago) TDD. Mockist (London) styles rot fast in AI-driven codebases.
4. Fewer meaningful tests beat many leaky ones.

## Mocking Rules

Mock only these:

- Database / ORM
- Third-party HTTP APIs
- Filesystem, clock, randomness, network
- Anything crossing a process boundary

Never mock these:

- Value objects, DTOs, entities you own
- Pure functions and utilities
- Internal collaborators such as services or modules in the same codebase
- The unit under test

Prefer an HTTP-level fake such as `wiremock`, `msw`, or `nock` over a trait or interface mock.
Prefer a real temp filesystem such as `tempfile` or `tmp.dirSync()` over a mocked `fs`.

## Assertion Rules

- Assert on return values and observable state.
- Do not make `toHaveBeenCalledWith(...)`, `verify(...)`, or `expect(spy).toBe(...)` the primary verification.
- Compare whole objects over field-by-field assertions when the comparison stays readable.
- Never snapshot nondeterministic output such as LLM text, timestamps, or ordering-free sets.

## Naming Rules

Test names must state observable behavior, not method names or internal calls.

Bad:

```text
test_findUnique_called_once()
test_calls_upsert_then_emits_event()
should_work()
```

Good:

```text
returns_cached_result_when_fetched_within_ttl()
rejects_login_when_password_is_expired()
charges_full_price_for_non_vip_users()
```

Template: `<subject>_<expected_behavior>_when_<condition>`

## Structure Rules

| Layer | Purpose | Budget |
| --- | --- | --- |
| Unit | Pure logic, entities, utils | Many, in-memory, milliseconds |
| Integration | Module plus real DB or queue | Moderate, per critical module |
| E2E | Critical user journeys | Few, one per journey |
| Regression | One per past incident | As bugs happen |

- Keep one E2E per critical journey.
- Keep a handful of integration tests per domain.
- Write unit tests only where logic is non-trivial.
- Skip unit tests for getters, DI wiring, or framework glue.
- Colocate unit specs near source when the codebase prefers that pattern.
- Keep integration and E2E in a separate tree when the project already separates them.
- Gate expensive live tests behind an env flag such as `LIVE_TEST=true` or `RUN_EXPENSIVE=1`.

## Domain Entity Rules

Extract a domain entity when any of these are true:

- Business logic is scattered across two or more services on the same data.
- A service performs arithmetic or state transitions on a plain DB row.
- You need a DB just to test logic that is really pure.

Before:

```text
user.hunger = user.hunger - EAT * 2
user.energy = user.energy + SLEEP * 2
db.user.update(user)
```

After:

```text
user.eat()
user.sleep()
user_repo.save(user)
```

Then `User.eat()` becomes a fast in-memory unit test instead of a DB-bound service test.

## Property-Based Testing

Use property-based tests alongside example tests when the code has a clear invariant over a wide input space.

Libraries:

- TypeScript: `fast-check`
- Python: `hypothesis`
- Rust: `proptest`

Rule: if you are writing the fourth example test for the same function, switch to a property.

## Flaky Test Rules

1. Never commit a flaky test. If one lands, quarantine it within 24 hours.
2. Quarantine means skip it with a linked issue, owner, and deadline. No owner means delete it.
3. Fix flakiness at the root, never with retries, `sleep()`, or larger timeouts.
4. Common roots are shared global state, real clocks, test ordering, unseeded randomness, and network access.

## Migration Rules For Existing Mockist Code

Do not rewrite existing tests for sport. Apply the policy incrementally:

1. New tests from today onward follow the rules fully.
2. Touched files should move mocks out to the boundary only.
3. Prioritize the worst files with the highest density of interaction assertions.
4. Introduce a real DB pattern in one high-risk domain first, then expand after the pattern proves itself.
5. Delete nondeterministic snapshot tests or replace them with structural assertions.

## Workflow Rules

- Write the failing test from the spec first, then implement against it.
- Do not generate code first and ask an agent to backfill tests.
- Keep one behavior per test.
- Multiple assertions are fine when they describe the same behavior.

## PR Red Flags

Reject or rework when you see:

- More `mock.*` calls than real assertions
- `toHaveBeenCalledWith(...)` or `verify()` as the only assertion
- Imports reaching into `_internal/` or other private module paths
- Snapshots of LLM, timestamp, or network output
- `it.skip` without a linked issue and owner
- Tests renamed whenever the function name changes
- A test file longer than the file it tests for a module with one public function
- New full-framework mocks added instead of a boundary mock or real DB

## When Not To Write A Test

- Plain CRUD with no logic: one E2E is enough
- Framework wiring such as DI, routing, or modules
- Config or constants that are already guarded by types or schema validation
- Throwaway scripts unless they touch production data
- Code that is about to be deleted

If you cannot state the protected behavior in one sentence, do not write the test.

## One Line To Remember

> Hide the implementation from the test. Hide the test from the implementation. Only behavior connects them.
