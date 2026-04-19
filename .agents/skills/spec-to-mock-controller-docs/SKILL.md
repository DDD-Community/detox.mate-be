---
name: spec-to-mock-controller-docs
description: Create mock Spring controllers, record DTOs, controller tests, REST Docs snippets, openapi3.yaml output, and Swagger UI wiring from API specs pasted as plain text. Use when Codex is asked things like "API 명세로 mock API 만들어줘", "텍스트 명세 기반 controller/rest docs/swagger-ui 만들어줘", "mock controller 문서화", or when a user provides HTTP method, path, request example JSON, and success response example JSON and wants an endpoint implemented in this repository's existing REST Docs pipeline.
---

# Spec To Mock Controller Docs

## Overview

Turn pasted API text specs into repository-shaped mock endpoints. Implement controller-only mocks, keep DTOs as Java `record`s, and extend the existing `REST Docs -> openapi3.yaml -> Swagger UI` pipeline instead of inventing a parallel one.

## Inputs

Require these minimum inputs before implementing:

- HTTP method
- path
- request example JSON when the endpoint has a request body
- success response example JSON

Read [references/spec-contract.md](references/spec-contract.md) only when you need the exact contract, examples, or fallback defaults.

If any required input is missing, do not implement yet. Ask only for the missing field in one short message.

## Workflow

1. Normalize the pasted spec into an endpoint checklist: method, path, request body presence, success status, request fields, and response fields.
2. Read [`../controller-api-docs/SKILL.md`](../controller-api-docs/SKILL.md) and [references/repo-pattern.md](references/repo-pattern.md) before editing code. Reuse that documentation pipeline rather than re-describing it from scratch.
3. Infer the target controller grouping from the path.
Path prefixes usually map to controller families such as `/auth` -> `AuthController`, `/users` -> `UserController`.
If no suitable existing controller exists, add a new controller in the repository's current package style.
4. Infer DTO `record` fields from the request and response example JSON.
Use request and response DTO names that match the endpoint intent and keep names stable for REST Docs schema generation.
5. Implement a controller-only mock.
Return canned response DTOs directly from the controller.
Do not add a service stub in v1 unless the user explicitly asks for it.
6. Add or update a standalone `MockMvc` controller test.
Document the success case first.
Add an error case only when the user explicitly asks for it, the existing controller family already documents one by default, or the success flow cannot be understood without it.
Use the same `document(...)` plus `resource(...)` pattern as the repository baseline.
7. Add the Asciidoc operation entries so the endpoint appears in generated docs.
8. Verify with `./gradlew test openapi3 copyOpenapi3Spec`.

## Rules

- Keep the source of truth in tests. Do not add Swagger annotations to controllers.
- Reuse the repository's `GlobalExceptionHandler` and `ErrorResponse`. Do not invent a new error envelope.
- Default to `200 OK`. Use `201 Created` only when the spec clearly describes resource creation.
- For bodyless `GET` and `DELETE` endpoints, omit request body DTO generation unless the spec explicitly requires one.
- Keep operation naming consistent between success and error snippets so OpenAPI generation stays stable.
- Prefer the smallest successful mock that satisfies the pasted spec.
- Do not add an error test by default just because validation annotations make it easy.

## Verification

- Run `./gradlew test openapi3 copyOpenapi3Spec`.
- Confirm the endpoint appears in `build/generated-snippets/**/resource.json`.
- Confirm the endpoint appears in `build/api-spec/openapi3.yaml`.
- If the repository normalizes or post-processes the generated spec, verify the final normalized `build/api-spec/openapi3.yaml` instead of relying on pre-normalization output.
- Inspect each generated OpenAPI path, not just path existence.
Check the HTTP method, status code, requestBody schema ref, response schema ref, path/query/header parameters, and operationId.
- Inspect the generated component schema or inline schema fields for the endpoint.
Check that required fields, nested object fields, array item fields, nullable fields, scalar types, enum values, default values, and field descriptions are all present and match the spec.
- For scalar values, confirm the OpenAPI type is as specific as the contract requires.
Examples: `minutes` should be `integer` when the contract says minutes in whole numbers, and `id` fields should not silently degrade to free-form `string`.
- For collection fields, confirm the empty-array contract when the endpoint allows an unset or not-yet-configured state.
Examples: verify whether `goalTimes` is documented as `[]`-capable and whether a default empty array is encoded when the repository pattern expects it.
- For enum-like strings, do not stop at `type: string`.
If the spec or PRD defines allowed values, verify they are emitted as OpenAPI `enum` values.
- If a list response is emitted as an anonymous schema instead of a named component, still inspect the item fields in the generated schema.
- Confirm the generated spec is still consumed through `/openapi3.yaml`.
- When visually checking Swagger, prefer `/swagger-ui/index.html` and cross-check the raw `/openapi3.yaml` content instead of trusting a cached browser view alone.
- If subagents are available and explicitly allowed for the session, forward-test once with a simple POST spec after major changes to this skill.

## Output

- State which controller, DTOs, test file, and Asciidoc entries were created or updated.
- State which Gradle command verified the result.
- State whether OpenAPI field-level verification was completed, and mention any mismatch such as missing schema refs, wrong parameter registration, or unexpectedly inlined schemas.
- If validation could not be completed, say exactly what was not verified.
