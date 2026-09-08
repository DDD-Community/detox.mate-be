# Refactor Decisions

## ACCEPTED

### DM-P5-002

- Add RED domain tests for valid creation/update, invalid user/name/minutes, 0/1440 boundaries, and failed update preserving prior state.
- Add Java-level domain validation in `App` and `AppTimeLimit` only after the RED is recorded.

### DM-P5-003

- Add repository/JPA integration tests for aggregate persistence, update without a second child, cascade child deletion, and preserving the existing user.
- Flush and clear the persistence context before assertions.

## REJECTED

### DM-P5-001 code change

- Reason: the repository's established `CurrentUserResolver` treats a token without a database user as unauthenticated and returns 401 before endpoint code. App-lock should not redefine this global authentication contract.
- Decision: correct the acceptance criterion from missing user 404 to invalid/missing authenticated user 401. Keep `UserRepository` loading in create because the existing `User` entity is required to form the aggregate relation.

## DEFERRED

### DM-P5-003 MySQL execution

- The manual MySQL DDL will be inspected and shipped, but this repository currently runs automated JPA tests on H2. Executing the script against a disposable MySQL/Testcontainers environment is deferred because no such test harness exists in the current scope.

### DM-P5-004

- Reason: the committed `applock/controller|service|dto` skeleton and surrounding repository use this legacy-compatible style. Adding commands, results, adapters, domain exception translation, and global-handler changes would expand this CRUD beyond its current complexity.

## Return To RED

DM-P5-002 requires a new RED-GREEN cycle before Phase 7 can pass. DM-P5-003 adds test hardening after the aggregate invariants are GREEN.
