# Acceptance criteria

## Normal
- Authenticated PUT /me/time-limit {totalLockMinutes:120} returns 200 and saves a single row owned by that user. Subsequent PUT replaces the value, including identical repeated requests.
- GET returns current value; DELETE removes only the owner setting and is idempotent 204, preserving users and other settings.
- Concurrent first saves complete successfully and leave one current row per user.

## Failure and boundaries
- Missing/malformed/nonexistent/withdrawn-user authentication: 401 and no mutation.
- Null/missing/negative/>1440 time: 400 and old state preserved; 0 and 1440 allowed.
- Unset or deleted setting GET:404 (distinct from configured zero).
- Domain rejects invalid values before state changes. Database enforces unique user and valid FK.

## Regression
- No App/AppTimeLimit or /me/apps CRUD remains in live production model. New JSON response carries no app identity.
- Activity records and notifications outside this narrow request remain functioning.
- Manual MySQL migration creates time_limits before cleanup, does not infer a total from per-app limits, and separates destructive cleanup after old backend removal.

## RED
New HTTP test references only existing Spring/JWT/User APIs. First PUT expects 200 but old implementation returns 404. Test further checks repeat updates and SQL count=1 after GREEN. In-scope user WIP compilation failures are separately recorded and never counted as RED.
