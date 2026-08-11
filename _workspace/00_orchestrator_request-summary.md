# App Lock CRUD Request Summary

## Requirement Scope

- Reuse the existing `User` entity and `users` table.
- Model `User 1:N App 1:1 AppTimeLimit`.
- Accept `userId`, `appDisplayName`, and `dailyLimitMinutes` from the frontend when creating and updating an app lock.
- Verify the requested `userId` matches the authenticated `CurrentUser` before accessing private app-lock data.
- Implement create, list, get, update, and delete through one `AppController` resource API.
- Persist `App` and `AppTimeLimit` together and delete the time limit with its owning app.

## API Scope

- `POST /me/apps`
- `GET /me/apps`
- `GET /me/apps/{appId}`
- `PUT /me/apps/{appId}`
- `DELETE /me/apps/{appId}`

Create and update requests contain the frontend `userId`. List, get, and delete derive ownership from `CurrentUser` and never accept another user's identifier.

## DDD Decision

- bounded context: `applock`
- aggregate root: `App`
- owned child entity: `AppTimeLimit`
- external aggregate reference: existing `User`
- command paths: create, update, delete
- query paths: list, get
- presentation boundary: `applock/controller` and `applock/dto`
- transaction orchestration: `applock/service`
- persistence boundary: `applock/repository`

The committed `applock/controller|service|domain|repository|dto` skeleton follows the repository's legacy-compatible domain-first structure. No broad package migration will be performed.

## Aggregate Rules

- `App.create(...)` creates a valid app and its time limit as one object graph.
- `App.update(...)` changes app display information and delegates limit changes to `AppTimeLimit`.
- `dailyLimitMinutes` must be between 0 and 1440 inclusive.
- `appDisplayName` must be non-blank and at most 100 characters.
- `AppTimeLimit` cannot be created or changed through a public controller/service of its own.
- `AppRepository` is the only aggregate repository; persistence uses cascade and orphan removal.

## Security And Error Rules

- A request `userId` different from `CurrentUser.id()` returns `403 Forbidden`.
- A missing user returns `404 Not Found`.
- A missing app or another user's app returns `404 Not Found`.
- Invalid request fields return `400 Bad Request`.

## Candidate Production Files

- `src/main/java/com/detoxmate/applock/domain/App.java`
- `src/main/java/com/detoxmate/applock/domain/AppTimeLimit.java`
- `src/main/java/com/detoxmate/applock/repository/AppRepository.java`
- `src/main/java/com/detoxmate/applock/service/AppService.java`
- `src/main/java/com/detoxmate/applock/controller/AppController.java`
- `src/main/java/com/detoxmate/applock/dto/AppCreateRequest.java`
- `src/main/java/com/detoxmate/applock/dto/AppUpdateRequest.java`
- `src/main/java/com/detoxmate/applock/dto/AppResponse.java`
- `src/main/java/com/detoxmate/applock/dto/AppListResponse.java`
- `db/manual/2026-08-11-app-lock.sql`

## Candidate Test Files

- `src/test/java/com/detoxmate/applock/domain/AppTest.java`
- `src/test/java/com/detoxmate/applock/repository/AppRepositoryTest.java`
- `src/test/java/com/detoxmate/applock/service/AppServiceTest.java`
- `src/test/java/com/detoxmate/applock/controller/AppControllerTest.java`

## Write Ownership

- test-designer: only new `src/test/java/com/detoxmate/applock/**` files during RED
- implementer: listed `src/main/java/com/detoxmate/applock/**`, related app-lock tests, and the app-lock manual DDL after valid RED
- refactor-reviewer: read-only review of the current diff
- test-auditor: read-only coverage audit
- orchestrator: `_workspace/00_*`, `_workspace/02_*`, `_workspace/06_*`, `_workspace/08_*`

Parallel writes are forbidden because the aggregate, service, controller, and tests share one vertical feature boundary.

## Verification Commands

```bash
./gradlew test --tests "com.detoxmate.applock.*"
./gradlew test
./gradlew clean build
git diff --check
git status
git diff
```
