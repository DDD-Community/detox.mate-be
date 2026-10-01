# GREEN: user time limits

## Implemented
- Replaced App/AppTimeLimit and 9 old production classes with TimeLimit, repository, service, controller, request, response. Removed three obsolete App tests.
- table time_limits directly references user with UNIQUE and FK. Time values validated in domain, HTTP DTO, and DB (0..1440). Physical user deletion cascades via SQL and matching JPA mapping.
- PUT/GET/DELETE /me/time-limit accept/return totalLockMinutes only. PUT replaces current value; GET unset404; DELETE idempotent204. Ownership from authenticated CurrentUser.
- First-set concurrency and writes serialize on existing UserRepository.findByIdForUpdate, with active-state check before mutation.
- Manual schema creation and later destructive cleanup are separate scripts. No data inference/backfill, no live DB execution.
- New real HTTP/REST Docs, domain, real JPA constraint and transaction/concurrency tests.

## Verification
- Implementer ran first HTTP requirements RED test: PASS, BUILD SUCCESSFUL in12s.
- Orchestrator confirmed additional physical FK deletion RED, then added @OnDelete(CASCADE).
- Orchestrator ran ./gradlew test --tests 'com.detoxmate.applock.*': PASS, BUILD SUCCESSFUL in13s, {'tests': 49, 'failures': 0, 'errors': 0, 'skipped': 0}.
- Test DB: H2 MySQL mode. MySQL-specific lock/isolation behavior and manual migrations are not verified against a running production DB.

## Ownership handoff
Implementer finished file edits and returned execution ownership. Orchestrator owns any subsequent edits; reviewers must inspect untracked new files as well as tracked diff.
