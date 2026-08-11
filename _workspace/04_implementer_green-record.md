# First GREEN Record

## RED Basis

- accepted RED record: `_workspace/03_test-designer_red-record.md`
- target test: `AppControllerTest.create_returnsCreatedApp_whenAuthenticatedRequestIsValid`
- recorded RED: expected `201 Created`, actual `404 Not Found`; test compilation succeeded and production had not been changed

## Package And Aggregate Decision

- bounded context: `applock`
- aggregate root: `App`
- owned child: `AppTimeLimit`
- external aggregate relation: existing `User`
- legacy-compatible packages preserved: `applock/controller|dto|service|domain|repository`
- command path: `AppController` -> transactional `AppService` -> `AppRepository` -> `App` cascade to `AppTimeLimit`

## Minimum GREEN Implementation

- added only `POST /me/apps`; list/get/update/delete remain unimplemented
- resolved the authenticated user through the existing `CurrentUser` argument
- loaded the existing `User` in the transactional service and saved one `App` aggregate
- made `App.create(...)` construct its owned `AppTimeLimit`
- mapped `App` to `User` and mapped `AppTimeLimit` as a one-to-one child persisted by `App` cascade with orphan removal
- added `AppCreateRequest` and `AppResponse` for the HTTP boundary
- did not add an `AppTimeLimit` repository, service, or controller
- did not modify tests or unrelated files
- intentionally deferred request-user mismatch handling, validation failures, and remaining CRUD behavior to later RED cycles

## Commands And Results

1. `./gradlew test --tests "com.detoxmate.applock.controller.AppControllerTest.create_returnsCreatedApp_whenAuthenticatedRequestIsValid"`
   - first sandboxed attempt: not executed because Gradle could not open its lock under `~/.gradle` (`Operation not permitted`)
   - approved retry: `BUILD SUCCESSFUL in 11s`; exit code `0`; target test GREEN
2. `./gradlew test --tests "com.detoxmate.applock.*"`
   - `BUILD SUCCESSFUL in 9s`; exit code `0`; related applock tests GREEN
3. `./gradlew test`
   - `BUILD SUCCESSFUL in 36s`; exit code `0`; full regression suite GREEN
4. `./gradlew clean build`
   - `BUILD SUCCESSFUL in 46s`; exit code `0`; clean build GREEN

## Result

First GREEN achieved for the authenticated valid-create flow. No review or refactor findings were applied in this phase.

---

# Full GREEN Record

## RED Basis

- accepted RED record: `_workspace/03_test-designer_red-record.md`
- target test class: `AppControllerTest`
- recorded RED: test compilation succeeded; 22 tests ran and 16 failed because create ownership/validation and list/get/update/delete behavior were missing

## Full GREEN Implementation

- added Jakarta request validation for required `userId`, non-blank app names of 1..100 characters, and daily limits of 0..1440 minutes
- made `CurrentUser.id()` authoritative and return `403 Forbidden` when a create/update request names another user
- added user-scoped repository queries for lists and individual aggregate access
- added current-user-only list/get/update/delete service and controller flows
- return the same `404 Not Found` for missing and other-user apps
- added `App.update(...)`, which changes the display name and delegates the limit change to its owned `AppTimeLimit`
- update uses JPA dirty checking and does not call repository `save`
- delete returns `204 No Content` and removes the child through aggregate cascade/orphan removal
- added repository-convention `created_at` and `updated_at` timestamps to both JPA entities
- added `db/manual/2026-08-11-app-lock.sql` with MySQL-compatible tables, the users foreign key, unique app time-limit foreign key, indexes, checks, and delete cascades
- did not add an `AppTimeLimit` repository, service, or controller
- did not modify tests or unrelated code

## Commands And Actual Results

1. `./gradlew test --tests "com.detoxmate.applock.controller.AppControllerTest"`
   - `BUILD SUCCESSFUL in 11s`; exit code `0`; all 22 controller tests GREEN
2. `./gradlew test --tests "com.detoxmate.applock.*"`
   - invoked after the controller suite passed, but the command was interrupted by the user before Gradle returned a separate result
   - the current applock test surface consists of `AppControllerTest`, which was already fully GREEN in command 1
3. Full suite/build
   - intentionally not rerun in this phase per the final orchestrator instruction

## Result

Full AppController behavior is GREEN. The broader selector has no known additional applock test class beyond the passing controller suite, but its redundant command did not produce a separate completion result before interruption.

---

# DM-P5-002 Accepted Refactor GREEN Record

## Accepted Finding

- add Java-level `IllegalArgumentException` preconditions for required `User`, non-null/non-blank app display names of at most 100 characters, and daily limits from 0 through 1440
- ensure `App.update(...)` validates every incoming value before mutating the aggregate so a failed update preserves the prior name, owned child identity, and limit
- keep `AppTimeLimit` creation/change package-private and validate in the child as defensive enforcement

## Implementation

- `App` validates `User` and display name before aggregate creation
- `App.update(...)` validates the display name and daily limit before assigning either value
- `AppTimeLimit.create(...)` and `changeDailyLimitMinutes(...)` remain package-private
- `AppTimeLimit` validates its parent and daily limit during creation and revalidates the limit during changes
- no tests or unrelated production files were modified

## Commands And Actual Results

1. `./gradlew test --tests "com.detoxmate.applock.domain.AppTest"`
   - `BUILD SUCCESSFUL in 4s`; exit code `0`; all 21 domain tests GREEN
2. `./gradlew test --tests "com.detoxmate.applock.controller.AppControllerTest"`
   - invoked, but interrupted by the user before Gradle returned output or an exit code
   - result: not independently verified in this phase; no test failure was reported before interruption
3. Additional commands
   - not run per the final orchestrator instruction

## Result

DM-P5-002 domain RED is GREEN. Controller regression verification remains without a completed command result because its invocation was interrupted.
