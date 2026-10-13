# Test Audit: random transfer phrase

## Coverage Map
| Acceptance criterion | Test / evidence | Status |
| --- | --- | --- |
| Authenticated exact stored phrase, Unicode/whitespace/newline/punctuation/attribution/no-store | get_returnsStoredPhraseWithoutChangingCatalogOrTimeLimit | pass |
| Single phrase repeated GET and duplicate allowance | get_returnsSamePhraseOnRepeatedRequestsWhenOnlyOneExists | pass |
| Sparse IDs / actual stored phrase selection | get_returnsOneStoredPhraseWhenIdsAreSparse | pass |
| Empty catalog standard404 | get_returnsNotFoundWhenCatalogIsEmpty | pass |
| Query current DB on every request | get_readsCurrentCatalogOnEachRequest | pass |
| Missing/malformed/unknown/withdrawn credentials401 | get_requiresExistingActiveAuthenticatedUser (4 cases) | pass |
| Catalog and time-limit persisted state unchanged | flush/clear and DB snapshot assertions | pass |
| No automatic seeding / write API / time change | production and manual SQL direct inspection | pass |

## Missing Tests
| Priority | Case | Reason / action |
| --- | --- | --- |
| blocking | None | Core behavior protected through HTTP and real DB assertions |
| recommended | None | Additional layer-duplicated tests unnecessary for this small read use case |
| deferred | Actual MySQL DDL/native query | Before deployment, validate manual DDL, schema validation and empty/single/sparse catalog reads in separate MySQL environment |

## Evidence and limits
- Independent agent phrase_test_audit read phase00..06, git diff HEAD, untracked tests/SQL, GREEN logs and JUnit XML.
- New9 + existing49 =58 tests,0 failures/errors/skipped.
- Real services/repositories/JWT/H2; no mock-count or probabilistic assertions.
- Every call executes RAND() LIMIT1, with no fixed ordering/cache, confirmed by source inspection. Statistical uniformity is not claimed.
- Read-only flow without writes/relationships/external clients needs no separate write-concurrency/rollback/outbound-failure tests.
- Agent ran no Gradle and changed no code; parent saved this returned report.
- Full tests and build are Phase8 work, not yet complete at audit time.

## Decision
- **pass**. No blocking gap, no return to RED required.
