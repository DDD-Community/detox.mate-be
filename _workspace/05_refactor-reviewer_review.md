# Refactor Reviewer Review

## Summary

- Critical findings: none
- Important findings: 3
- Suggestions: 1
- strengths: user-scoped repository predicates hide foreign-owned apps behind 404; `App` exclusively creates and mutates `AppTimeLimit`; JPA one-to-one uniqueness/cascade and manual DDL are structurally aligned.

## Findings

### DM-P5-001 - Missing-user HTTP contract is authentication-owned

- priority: Important
- category: correctness / authentication
- evidence: `CurrentUserResolver` resolves a database-backed user before the controller runs; a token subject without a `users` row is handled as 401 by the existing global authentication path. The Phase 1 criterion saying 404 is therefore unreachable at the HTTP boundary.
- recommendation: choose one contract. Preserve the existing authentication contract as 401 and correct the app-lock acceptance criterion.

### DM-P5-002 - Aggregate invariants exist only at the HTTP/DB boundaries

- priority: Important
- category: DDD invariants
- evidence: `App.create`, `App.update`, `AppTimeLimit.create`, and `changeDailyLimitMinutes` assign values without Java-level validation. Direct domain use can create an invalid aggregate.
- recommendation: add domain preconditions for required user, nonblank 1..100-character name, and 0..1440 minutes, with direct domain tests including failed-update state preservation.

### DM-P5-003 - Persistence contract is not directly verified

- priority: Important
- category: JPA / tests
- evidence: controller tests execute in one outer transaction and verify deletion only through HTTP. They do not flush/clear and prove one child row, dirty checking, child cascade deletion, or preservation of `User`.
- recommendation: add persistence integration tests that flush and clear around create/update/delete. Validate the manual DDL separately against MySQL when an integration environment is available.

### DM-P5-004 - Service depends on presentation DTOs and HTTP exceptions

- priority: Suggestion
- category: responsibility separation
- evidence: `AppService` accepts controller request records, builds response records, and throws `ResponseStatusException`.
- recommendation: introduce application commands/results and app-lock exceptions only when this context grows enough to justify the extra mapping and exception translation.

