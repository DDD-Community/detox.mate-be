# Request: random transfer phrase GET API

## Scope and product contract
- User requests one randomly selected DB phrase for the limit-change typing screen.
- Phrases are entered manually; no runtime seeding, write API, transcription validation, or time-limit changes.
- GET /transfer-phrases/random, authenticated through existing CurrentUser resolution.
- Response 200 with exactly {"phrase": "stored text"}; preserve full stored text, punctuation, whitespace and any attribution stored in the same string.
- Each request samples independently; consecutive duplicates allowed. Successful responses use Cache-Control: no-store.
- Empty phrase catalog returns existing 404 error envelope. Invalid/missing/withdrawn/unknown credentials return 401.
- Catalog currently has approximately 15 fixed phrases. No paging or large-catalog optimizations required.
- Prepare manual MySQL table DDL and operator/API documentation if needed; do not execute against actual DB or insert the supplied collection automatically.

## DDD / packages
- Existing in-progress bounded context: transferminute; aggregate: TransferPhrase, independently stored immutable catalog entry.
- Read/query use case, no business state transition or cross-aggregate modification.
- Retain existing domain/controller/service/repository/dto packages for this narrow completion; no broad package moves or speculative interfaces.
- Domain remains free of HTTP, DTO, and service dependencies. Controller maps service query result to response.

## Ownership and pipeline
- orchestrator: phase 00/02/06/08 records, src/docs/asciidoc/index.adoc and API/manual-DB guide.
- test-designer: src/test/java/com/detoxmate/transferminute/** and phase 01/03 records. Design first; RED after baseline approval.
- implementer: existing five src/main/java/com/detoxmate/transferminute/** files, db/manual/2026-10-05-transfer-phrase.sql and phase 04 only, after valid RED.
- reviewer: read-only independent review, phase 05 report only.
- auditor: read-only independent test audit, phase 07 report only.
- No parallel Gradle runs or concurrent writes to the same files. No commit/push/PR in this request.

## Initial state and validation
- Branch feat/transfer-limit; five staged skeleton files supplied by user. Preserve/complete these files. Initial index and working patches saved under _workspace/transfer-phrases-initial-*.patch.
- Prior harness records archived under _workspace/archive-before-transfer-phrases/.
- Related baseline: ./gradlew test --tests 'com.detoxmate.applock.*'
- New scope: ./gradlew test --tests 'com.detoxmate.transferminute.*'
- Final: ./gradlew test; ./gradlew clean build; ./gradlew asciidoctor; git diff --check; git diff --cached --check; git status; git diff HEAD.

## Query design reference
- MySQL official manual confirms ORDER BY RAND() with LIMIT for random sampling: https://dev.mysql.com/doc/refman/8.4/en/mathematical-functions.html#function_rand (checked 2026-10-05).
- For this approximately 15-row fixed catalog, prefer a single native read returning one real row; do not generate a random numeric primary key (manual inserts can have ID gaps).
- Preserve the current transfer_phrase table/column naming. IDENTITY generation with an explicit manual AUTO_INCREMENT DDL avoids AUTO sequence/table-generator prerequisites when manually inserting rows.
