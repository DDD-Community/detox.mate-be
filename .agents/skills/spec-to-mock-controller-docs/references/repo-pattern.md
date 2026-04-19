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
3. `copyOpenapi3Spec` copies the spec into static resources
4. Swagger UI reads `/openapi3.yaml`

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

## Naming Guidance

- Group controllers by leading path segment.
- Keep snippet names stable and readable, for example `auth/refresh` or `users/me-get`.
- Keep success and error snippet names aligned so the OpenAPI result stays coherent.
