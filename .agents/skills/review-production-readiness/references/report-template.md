# Production Readiness Report Template

Use this structure for the final answer. Keep it concise unless the user asks for a full audit document.

## Findings

For each issue:

```markdown
- [P0/P1/P2/P3] Short title
  Evidence: `path/to/File.java:123` and the exact behavior observed.
  Risk: What breaks under concurrency, two servers, production traffic, or load test.
  Trigger: The concrete condition that exposes it.
  Fix direction: The smallest defensible fix and any required test/metric.
```

## Must Fix Before Scaling To Two Servers

List only items that are unsafe in a horizontally scaled deployment: duplicated schedulers, server-local correctness state, local sessions, non-shared files, JVM-local locks for business invariants, or local caches controlling correctness.

## Load-Test Targets

Name the endpoints/jobs to test and the likely bottleneck:

- Endpoint/job:
- Scenario:
- Expected failure signal:
- Metric/log to observe:

## Verification Performed

State what was inspected: source paths, config files, tests, migrations, docs, and commands run. State important gaps separately.

## Suggested Fix Order

Order by risk and dependency, not by implementation convenience.
