# User time limits replacement

## Scope
Replace App/AppTimeLimit and /me/apps with one TimeLimit per authenticated user. Use table time_limits; no app names, IDs, or app relationship. Preserve the existing 0..1440 minute policy; field totalLockMinutes. PUT /me/time-limit upserts current value and returns 200; GET returns 200 or 404 when unset; DELETE returns idempotent 204. CurrentUser determines ownership. Existing app values cannot be safely converted and are not backfilled.

## Domain and packages
Existing applock bounded context, TimeLimit aggregate owns value validation and changeTotalLockMinutes. Keep domain, service, controller, dto, repository package names to avoid an unrelated package migration. Domain must not import HTTP or request DTOs. Unique user FK enforces one row; serialize writes with existing UserRepository.findByIdForUpdate to cover concurrent first save.

## Exclusions
Do not alter activityrecord types/usage goals, feed/calendar history, notification behavior, or soft-withdrawal retention. Do not execute production DB migration or create commits/PRs.

## Ownership
Orchestrator: _workspace synthesis, baseline/RED execution and final verification. Test designer: assigned new src/test/applock tests only after baseline. Implementer: src/main/java/com/detoxmate/applock/**, related src/test/java/com/detoxmate/applock/** after RED, db/manual new migration and usage documentation. Refactor reviewer and test auditor: read-only reports. No overlapping writes.

## Initial working tree
User has partial App/AppTimeLimit migration and AppController @Deprecated edits. Preserved in initial-user-changes.patch; intent is incorporated by replacing these components. Initial current-tree test failed compileJava because AppResponse still calls removed getAppDisplayName and AppService calls old create/update signatures. This is an in-scope interrupted transition, not requirements RED. Establish old-behavior baseline and valid HTTP RED in an isolated HEAD snapshot, then implement the authorized replacement in the current tree. Never revert user edits into the main checkout.
