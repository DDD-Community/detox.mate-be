# Acceptance Criteria: random transfer phrase

## Protected behavior and boundary
- Authenticated frontend requests `GET /transfer-phrases/random` and receives one complete phrase from the manually populated catalog.
- Bounded context: `transferminute`; aggregate: immutable catalog entry `TransferPhrase`.
- Keep existing domain/controller/service/repository/dto packages. No new domain policy or random-provider abstraction is required for this database query.
- Test the full HTTP-to-real-database path with `@SpringBootTest`, MockMvc, H2 MySQL mode, real JWT creation, current-user resolution, and transaction rollback.
- Test data is inserted directly into the DB because catalog population is an operator action, not an application write use case. Do not add production constructors or write endpoints merely for test setup.

## Normal
- Existing authenticated active user receives status 200 and exactly `{ "phrase": "stored text" }`; IDs or entity metadata are not exposed.
- Successful response declares `Cache-Control: no-store`.
- Read preserves the entire stored phrase, including leading/trailing spaces, Unicode, punctuation, line breaks, and attribution contained in that string.
- Repeated requests can return the same phrase; the catalog is not consumed and no per-user history is created.

## Failure
- Empty catalog returns 404 with the existing `NOT_FOUND` error envelope, never a hardcoded fallback or an empty success body.
- Missing token, malformed token, valid token for an unknown user, and valid token for a withdrawn user return 401.

## Boundary
- A single stored phrase is returned on repeated requests, proving duplicates are allowed and the row remains available.
- A catalog with several non-contiguous IDs returns exactly one of the current stored texts. Selection cannot assume IDs span `1..count`.
- Reading after an operator replaces the single catalog row returns the new row; the API does not retain a fixed application-cache result.

## Regression and state
- A successful GET leaves the catalog's complete persisted `(id, transfer_phrase)` rows unchanged.
- GET does not create or change any `time_limits` rows. This API supplies text only; typing validation and applying time changes stay outside its scope.
- There are no HTTP catalog create/update/delete operations. Do not add them. Specific unsupported-method status assertions are out of scope: the existing global handler currently turns unsupported methods into 500, and changing that unrelated shared behavior is not part of this feature.
- Use a real transaction and flush/clear before DB snapshots; no interaction-count assertions, service/repository mocks, or reflective entity mutation.

# RED Test Plan
- target file: `src/test/java/com/detoxmate/transferminute/controller/TransferPhraseControllerTest.java`
- first test: `get_returnsStoredPhraseWithoutChangingCatalogOrTimeLimit`
- smallest observable behavior: one inserted phrase, an active authenticated user, GET returns status 200 and the exact phrase.
- setup: insert an explicit non-contiguous catalog ID through `JdbcTemplate`, persist an active user through `UserRepository`, and insert an existing `time_limits` row to protect the separate feature.
- HTTP baseline should fail at expected 200 versus actual 404 while compiling successfully because the current controller has no mapped method.
- command for first RED: `./gradlew test --tests 'com.detoxmate.transferminute.controller.TransferPhraseControllerTest.get_returnsStoredPhraseWithoutChangingCatalogOrTimeLimit'`
- command for all new tests after implementation: `./gradlew test --tests 'com.detoxmate.transferminute.*'`
- baseline command (orchestrator owns execution): `./gradlew test --tests 'com.detoxmate.applock.*'`
- Final verification remains `./gradlew test`, `./gradlew clean build`, documentation generation, and Git diff checks.

## Planned test cases
1. Single result, exact original text and response schema, no-store, persisted catalog and existing time-limit preservation; also produces REST Docs `transfer-phrases/random` and OpenAPI `TransferPhraseResponse`.
2. Single-row repeated requests remain successful and return the same row.
3. Several sparse IDs produce a member of the stored text set; no probability-based expectation that all rows appear.
4. Empty catalog gives 404 with the established error response.
5. Operator replacement between requests is reflected on the next GET.
6. Parameterized unauthorized cases: missing/malformed/unknown/withdrawn credentials.

## Test limitations and review obligation
- Real database randomness is nondeterministic. Membership and sparse-ID tests prove valid retrieval but cannot prove uniform probability or rule out an implementation that always picks the first row.
- Do not add `N` requests hoping every phrase appears, fixed-seed coupling to an SQL implementation, sleeps, or a production abstraction solely to make SQL randomness mockable.
- Reviewer must inspect that every request executes an actual random selection (for example `ORDER BY RAND() LIMIT 1` for this approximately 15-row immutable catalog), without a fixed ordering or application cache.
- H2 MySQL mode validates the executed query and HTTP flow locally; actual MySQL DDL/query verification is a deployment check, not claimed by these tests.
- No getter/DTO unit tests or repeated service/repository integration suites are needed for this simple read use case.

## Phase status
- Phase 1 design completed without production/test edits or Gradle execution.
- Orchestrator recorded 49 baseline tests GREEN in phase 02 and authorized Phase 3.
- Phase 3 now has nine test invocations in one real HTTP/DB suite (five normal/boundary tests and four authentication cases); no production files changed.
- Orchestrator owns RED execution so Gradle cache approval and task ownership remain serial.
