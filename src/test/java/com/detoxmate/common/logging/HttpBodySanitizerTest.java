package com.detoxmate.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpBodySanitizer")
class HttpBodySanitizerTest {

    private final HttpBodySanitizer sanitizer = new HttpBodySanitizer();

    @Test
    @DisplayName("민감한 필드는 중첩 객체와 배열에서도 마스킹한다")
    void masks_sensitive_fields_in_nested_objects_and_arrays() {
        // given
        String body = """
                {
                  "identityToken": "apple-id-token",
                  "profile": {
                    "password": "secret-password",
                    "items": [
                      {"authorizationCode": "apple-code"},
                      {"name": "visible"}
                    ]
                  }
                }
                """;

        // when
        String sanitized = sanitizer.sanitize(body);

        // then
        assertThat(sanitized).contains("\"identityToken\":\"***\"");
        assertThat(sanitized).contains("\"password\":\"***\"");
        assertThat(sanitized).contains("\"authorizationCode\":\"***\"");
        assertThat(sanitized).contains("\"name\":\"visible\"");
        assertThat(sanitized).doesNotContain("apple-id-token", "secret-password", "apple-code");
    }

    @Test
    @DisplayName("로그인 응답의 토큰 필드는 마스킹한다")
    void masks_login_response_token_fields() {
        // given
        String body = """
                {
                  "accessToken": "access-token-value",
                  "refreshToken": "refresh-token-value",
                  "isNewUser": false
                }
                """;

        // when
        String sanitized = sanitizer.sanitize(body);

        // then
        assertThat(sanitized).contains("\"accessToken\":\"***\"");
        assertThat(sanitized).contains("\"refreshToken\":\"***\"");
        assertThat(sanitized).contains("\"isNewUser\":false");
        assertThat(sanitized).doesNotContain("access-token-value", "refresh-token-value");
    }

    @Test
    @DisplayName("긴 body와 긴 문자열은 잘라서 남긴다")
    void truncates_long_body_and_long_string_fields() {
        // given
        String longValue = "a".repeat(300);
        String body = """
                {
                  "memo": "%s",
                  "description": "%s"
                }
                """.formatted(longValue, longValue);

        // when
        String sanitized = sanitizer.sanitize(body);

        // then
        assertThat(sanitized).contains("...[truncated]");
        assertThat(sanitized.length()).isLessThanOrEqualTo(2048 + "...[truncated]".length());
    }

    @Test
    @DisplayName("파싱할 수 없는 JSON은 원문을 남기지 않는다")
    void hides_raw_body_when_json_is_unparseable() {
        // when
        String sanitized = sanitizer.sanitize("{\"accessToken\":\"secret\"");

        // then
        assertThat(sanitized).isEqualTo("[unparseable-json]");
    }
}
