# Production Readiness Risk Checklist

Use this checklist after the initial codebase scan. Load only the relevant sections if the request is narrow.

## 1. Multi-Server Safety

- In-memory mutable state used for business correctness: `static` collections, singleton fields, local counters, local rate limiters, local token/session stores, local deduplication maps.
- File-system assumptions: local uploads, local temp files used after request completion, generated files expected by another server, non-shared path dependencies.
- Scheduler duplication: `@Scheduled`, Quartz, batch jobs, polling consumers, cleanup jobs, notification jobs, settlement jobs, or reward jobs that would run once per instance.
- WebSocket/SSE state: connected-user maps, room membership, fanout logic, and cross-node delivery.
- Session/auth state: sticky-session assumptions, server-local logout/token revocation, server-local refresh-token tracking.
- Cache topology: local cache where correctness needs global invalidation, cache stampede risk, missing TTL, or stale authorization/entitlement data.

## 2. Concurrency and Data Consistency

- Check-then-act races: `exists`/`count`/`find` followed by `save`, state transition, payment, reward, join, like, coupon, inventory, or quota update.
- Lost update risk: read-modify-write on counters, balances, inventory, progress, score, remaining quantity, or status without versioning, atomic update, or lock.
- Missing idempotency: payment callbacks, external webhooks, retries, message consumers, notification sends, challenge completion, reward issuance, file processing.
- Transaction boundary mismatch: multiple writes across aggregates without a clear transaction, external calls inside transactions, event publication before commit, async execution reading uncommitted assumptions.
- Lock misuse: JVM-local `synchronized`/`ReentrantLock` used for cross-server invariant, Redis lock without expiry/owner token, DB lock held while calling external services.
- Constraint gaps: uniqueness or state invariants enforced only in Java code when concurrent requests can bypass them.
- Isolation assumptions: relying on default `READ_COMMITTED` while expecting repeatable reads or serial behavior.

## 3. Database and JPA Load Risks

- N+1 query paths: loops over entities with lazy associations, DTO mapping that dereferences relations, serialization of entities, collection access in response builders.
- Unbounded queries: no pagination, no limit, no date/window filter, loading all records to count/filter/sort in memory.
- Repository calls inside loops: per-row lookup, save, delete, or external enrichment instead of batching or join queries.
- Index gaps: filters, joins, ordering, uniqueness checks, foreign key lookups, and time-range scans without supporting indexes.
- Pagination instability: offset pagination on large tables, non-deterministic sort, missing tie-breaker, or user-visible duplicate/missing rows during writes.
- Connection pool/thread pool mismatch: blocking external calls or long transactions tying up DB connections under load.
- Entity serialization: exposing entities directly from controllers, lazy-loading during JSON serialization, or recursive object graphs.
- Bulk writes: per-entity dirty checking for large batches when bulk update/insert or chunking is required.

## 4. External Integration Resilience

- Missing timeouts on HTTP clients, SDK clients, database clients, Redis, or message brokers.
- Retry without idempotency, backoff, jitter, retry limit, or circuit breaking.
- External calls inside DB transactions, especially slow APIs, file storage, notification vendors, payment providers, or LLM APIs.
- No compensation path for partial failure after local commit or after external side effect.
- Secrets/config hardcoded or environment-specific assumptions hidden in code.
- Rate limits not modeled for third-party APIs.

## 5. Load-Test Failure Points

- Endpoint does CPU-heavy work synchronously: password hashing loops, compression, parsing large files, image/video processing, report generation, AI calls.
- Large response payloads or nested DTOs without field selection or pagination.
- Synchronous fanout: sending many notifications, emails, webhook calls, or messages within a request.
- Logging hot paths with large payloads, stack traces, or synchronous appenders.
- Missing backpressure: unbounded queues, async executor default settings, unlimited file uploads, unlimited request body, or no rate limiting.
- Contention hot spots: one popular row, one counter, one Redis key, one lock, one scheduler job, or one DB index receiving most writes.

## 6. Observability and Operability

- No health/readiness distinction for dependencies.
- No metrics for request latency, DB pool, slow queries, cache hit ratio, queue lag, executor queue size, scheduler duration, external API latency/error rate.
- Errors swallowed in async jobs, event listeners, or schedulers.
- Missing structured logs around state transitions, idempotency decisions, and external request identifiers.
- No migration/rollback awareness for schema changes that affect large tables or constraints.

## 7. Fix Patterns to Prefer

- Use unique constraints plus duplicate-key handling for one-time creation.
- Use optimistic locking for normal concurrent edits where retries are acceptable.
- Use atomic SQL updates for counters, inventory, quota, and claim patterns.
- Use pessimistic locking only around short DB-only critical sections.
- Use idempotency keys for retries, callbacks, and message consumers.
- Use queue or outbox patterns for external side effects after commit.
- Use distributed locks only when DB constraints/claim rows/queues cannot express the invariant cleanly.
- Use cursor pagination and stable ordering for growing datasets.
- Add indexes based on query predicates and sort order, then verify with explain plans or slow-query evidence.
