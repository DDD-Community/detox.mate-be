# Spec Contract

Use this reference when the pasted API spec is ambiguous or when you need to confirm minimum inputs before editing code.

## Minimum Required Fields

- `method`
- `path`
- `request example JSON` when the endpoint accepts a request body
- `success response example JSON`

The input can be free-form text as long as those facts are present.

## Accepted Input Shapes

Example with body:

```text
POST /auth/social/kakao
request
{
  "providerAccessToken": "kakao-access-token"
}

response 200
{
  "id": 1,
  "displayName": "카카오닉네임",
  "profileImageUrl": null
}
```

Example without body:

```text
GET /users/me
headers
Authorization: Bearer access-token

response 200
{
  "id": 1,
  "displayName": "카카오닉네임",
  "profileImageUrl": "https://example.com/profile.png"
}
```

## Missing Information

If a required field is missing, ask only for that field.

Examples:

- `HTTP method가 빠졌습니다. method만 알려주세요.`
- `성공 응답 예시 JSON이 필요합니다. 그 JSON만 보내주세요.`
- `요청 바디가 있는 endpoint라면 request example JSON이 필요합니다.`

Do not ask broad design questions if the missing detail can be inferred from the repository pattern.

## Defaults

- success status: `200 OK`
- creation status: `201 Created` only when the spec clearly implies resource creation
- error envelope: existing `ErrorResponse`
- error case: omit by default and add only when the user asks or the endpoint contract clearly depends on it
- request/response DTO style: Java `record`
- mock structure: controller-only canned response

## Error Case Selection

Skip this section unless an error case is actually needed.

If needed, prefer one of these, in order:

1. validation failure for malformed or blank request data
2. missing required header such as `Authorization`
3. unauthorized or forbidden access when clearly implied by the endpoint

Use the simplest case that fits the spec and the repository's existing `GlobalExceptionHandler`.
