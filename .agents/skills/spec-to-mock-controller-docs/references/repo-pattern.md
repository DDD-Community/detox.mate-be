# Repository Pattern

Use this reference before implementing. It summarizes the project-local conventions that this skill must follow.

## Files To Mirror

- [`src/test/java/com/detoxmate/user/controller/AuthControllerTest.java`](/Users/euijinkk/Desktop/projects/side-project/detox.mate-be/src/test/java/com/detoxmate/user/controller/AuthControllerTest.java)
- [`src/test/java/com/detoxmate/user/controller/UserControllerTest.java`](/Users/euijinkk/Desktop/projects/side-project/detox.mate-be/src/test/java/com/detoxmate/user/controller/UserControllerTest.java)
- [`build.gradle`](/Users/euijinkk/Desktop/projects/side-project/detox.mate-be/build.gradle)
- [`src/main/resources/application.yml`](/Users/euijinkk/Desktop/projects/side-project/detox.mate-be/src/main/resources/application.yml)
- [`src/docs/asciidoc/index.adoc`](/Users/euijinkk/Desktop/projects/side-project/detox.mate-be/src/docs/asciidoc/index.adoc)
- [`../../controller-api-docs/SKILL.md`](../../controller-api-docs/SKILL.md)

## Implementation Conventions

- Controller layer stays thin.
- DTOs are Java `record`s.
- Validation uses Jakarta annotations such as `@NotBlank`.
- Controller tests use `MockMvcBuilders.standaloneSetup(...)`.
- Tests apply `documentationConfiguration(...)` with `RestDocumentationExtension`.
- REST Docs metadata is attached in tests with `document(...)` and `resource(...)`.

## Documentation Pipeline

The repository already uses this pipeline:

1. controller test generates snippets and `resource.json`
2. `openapi3` task composes `build/api-spec/openapi3.yaml`
3. `normalizeOpenapi3Spec` may post-process the generated OpenAPI document
4. `copyOpenapi3Spec` copies the final spec into static resources
5. Swagger UI reads `/openapi3.yaml`

Do not replace this pipeline. Extend it.

## What To Edit

- controller class for the endpoint behavior
- request/response DTOs when needed
- controller test for the success case first
- `src/docs/asciidoc/index.adoc` operation entries

Avoid touching `build.gradle` or `application.yml` unless the existing pipeline is missing or broken.

## Testing Shape

- Success case must be documented.
- Add an invalid-request or client-visible error case only when the user asks for it or the endpoint contract clearly needs it.
- Reuse field descriptors and header descriptors between REST Docs and `resource(...)`.
- Stabilize schema names with `requestSchema(...)` and `responseSchema(...)` when request or response bodies exist.
- If nested arrays or object items are emitted inline, inspect the final normalized `build/api-spec/openapi3.yaml` and decide whether repository-specific post-processing is needed to stabilize component refs.
- If path/query/header parameter types degrade in generated output, verify the final spec instead of assuming the test descriptors were enough.

## Naming Guidance

- Group controllers by leading path segment.
- Keep snippet names stable and readable, for example `auth/refresh` or `users/me-get`.
- Keep success and error snippet names aligned so the OpenAPI result stays coherent.

## Verification Focus

- Treat the final normalized `build/api-spec/openapi3.yaml` as the OpenAPI source of truth when this repository applies post-processing.
- If anonymous schema names, nested item refs, or parameter scalar types look wrong in Swagger UI, inspect `build/api-spec/openapi3.yaml` before changing controller code.
- When verifying the rendered docs, check both `/swagger-ui/index.html` and `/openapi3.yaml`.
- Be cautious about browser cache when using Swagger UI; the raw YAML is the more reliable debugging surface.
