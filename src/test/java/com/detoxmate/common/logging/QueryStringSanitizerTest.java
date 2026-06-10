package com.detoxmate.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QueryStringSanitizer")
class QueryStringSanitizerTest {

    private final QueryStringSanitizer sanitizer = new QueryStringSanitizer();

    @Test
    @DisplayName("민감한 query parameter를 마스킹하고 일반 parameter는 유지한다")
    void masks_sensitive_query_parameters_and_keeps_normal_parameters() {
        // when
        String sanitized = sanitizer.sanitize("code=apple-code&state=xyz&apiKey=secret-key&size=20");

        // then
        assertThat(sanitized).contains("code=***");
        assertThat(sanitized).contains("apiKey=***");
        assertThat(sanitized).contains("state=xyz");
        assertThat(sanitized).contains("size=20");
        assertThat(sanitized).doesNotContain("apple-code", "secret-key");
    }

    @Test
    @DisplayName("긴 query string은 잘라서 남긴다")
    void truncates_long_query_string() {
        // when
        String sanitized = sanitizer.sanitize("cursor=" + "a".repeat(1200));

        // then
        assertThat(sanitized).contains("...[truncated]");
        assertThat(sanitized.length()).isLessThanOrEqualTo(1024 + "...[truncated]".length());
    }

    @Test
    @DisplayName("query string이 없으면 빈 문자열을 반환한다")
    void returns_empty_string_when_query_string_is_missing() {
        // when & then
        assertThat(sanitizer.sanitize(null)).isEmpty();
        assertThat(sanitizer.sanitize("")).isEmpty();
    }
}
