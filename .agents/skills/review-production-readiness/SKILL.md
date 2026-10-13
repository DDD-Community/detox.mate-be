---
name: review-production-readiness
description: Review backend code for production readiness, concurrency safety, scale-out risks, and load-test failure points. Use when asked to find optimization opportunities, race conditions, non-distributed state, server-to-server scaling blockers, scheduler/cache/session/lock problems, database bottlenecks, N+1 queries, transaction issues, or parts likely to fail under real production traffic.
---

# Review Production Readiness

## Overview

Use this skill to review a backend codebase before production traffic or horizontal scaling. Focus on concrete failure modes: incorrect behavior with concurrent requests, duplicated work across two or more servers, fragile transactions, database hot spots, memory-only state, and bottlenecks that load tests would expose.

## Workflow

1. Establish the runtime shape before judging code.
   - Identify framework, persistence layer, cache, queue, scheduler, auth/session strategy, deployment assumptions, and external integrations.
   - Inspect configuration files, dependency declarations, database migrations, scheduled jobs, async executors, caching config, and transaction boundaries.
   - If the project has README, architecture docs, PRDs, or deployment files, read them first.

2. Build an evidence map.
   - Search for stateful or concurrency-sensitive patterns: `static`, in-memory maps/sets, local caches, synchronized blocks, locks, scheduled jobs, async/event listeners, transactions, retries, idempotency keys, unique constraints, batch jobs, and external API calls.
   - Search for performance-sensitive paths: controller endpoints, service methods with loops, repository calls inside loops, eager fetching, pagination gaps, bulk updates, file uploads, large JSON responses, and unbounded queries.
   - Prefer source evidence over speculation. Cite file and line for every finding.

3. Review by risk category.
   - Read `references/risk-checklist.md` for the detailed checklist.
   - For Spring Boot/JPA projects, pay special attention to transaction isolation, lost updates, optimistic/pessimistic locking, unique constraints, N+1 queries, lazy-loading at serialization boundaries, connection pool exhaustion, and scheduler duplication.
   - For Redis, queues, or external services, check timeout, retry, idempotency, and distributed lock semantics.

4. Rank findings by production impact.
   - P0: can corrupt data, double-charge/double-reward, violate security, or make multi-server deployment unsafe.
   - P1: likely to fail under ordinary production concurrency or load.
   - P2: measurable performance or operability risk, but with narrower blast radius.
   - P3: optimization or hardening opportunity with limited immediate risk.

5. Produce an actionable report.
   - Use `references/report-template.md` for output structure.
   - Include reproduction or validation ideas for high-risk findings: concurrent test, integration test, DB constraint, load-test scenario, or metric to observe.
   - Separate "must fix before 2 servers" from "optimize after measurement".
   - Do not recommend distributed locks or async processing by default. Explain why the simpler DB constraint, transaction boundary, idempotency key, or queue pattern is insufficient before proposing heavier infrastructure.

## Review Heuristics

- Treat "works locally" as insufficient. Ask whether the behavior still holds with two JVMs, two scheduler instances, repeated HTTP requests, retry storms, and partial external failures.
- Prefer database-enforced invariants for uniqueness and one-time actions. Application-level `exists` checks are not concurrency protection by themselves.
- Treat in-memory state as per-process only unless there is explicit replication or external persistence.
- Treat scheduled jobs as duplicated per server unless there is a leader election, distributed lock, queue claim, or database claim pattern.
- Treat cache invalidation as a correctness concern when cached data controls authorization, quota, inventory, membership, payment, rewards, or state transitions.
- For performance findings, identify the suspected scaling dimension: request rate, row count, payload size, cardinality, connection count, thread count, or external latency.

## Output Rules

- Lead with findings, not a long overview.
- Every finding must include evidence, impact, trigger condition, and a concrete fix direction.
- Avoid vague statements like "may have concurrency issues" without naming the exact race.
- If evidence is incomplete, label it as "needs verification" and state what to inspect next.
- Mention checks already performed and important gaps, especially when tests or runtime config were not available.
