package com.detoxmate.common.logging;

import com.detoxmate.auth.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("RequestLoggingFilter")
class RequestLoggingFilterTest {

    private static final String JWT_SECRET = "this-is-a-very-long-secret-key-for-request-logging-test";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, 3600L);
    private final RequestLoggingFilter filter = new RequestLoggingFilter(
            jwtTokenProvider,
            new HttpBodySanitizer(new ObjectMapper()),
            new QueryStringSanitizer(),
            new ApiLogWriter()
    );
    private final MockMvc mockMvc = standaloneSetup(new LoggingTestController())
            .addFilters(filter)
            .build();

    @Test
    @DisplayName("JSON 요청과 응답 body를 마스킹하고 requestId와 userId로 연결해 로그를 남긴다")
    void logs_masked_json_bodies_with_request_id_and_user_id(CapturedOutput output) throws Exception {
        // given
        String accessToken = jwtTokenProvider.createAccessToken(12L);

        // when & then
        mockMvc.perform(post("/echo?code=apple-code&size=20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("X-Request-Id", "req-existing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityToken": "apple-id-token",
                                  "memo": "visible"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-existing"))
                .andExpect(content().json("""
                        {
                          "accessToken": "response-access-token",
                          "memo": "visible"
                        }
                        """));

        assertThat(output).contains(
                "event=http_request_body requestId=req-existing userId=12 method=POST path=/echo body={\"identityToken\":\"***\",\"memo\":\"visible\"}",
                "event=http_response_body requestId=req-existing userId=12 method=POST path=/echo status=200",
                "\"accessToken\":\"***\"",
                "\"memo\":\"visible\"",
                "event=http_request requestId=req-existing userId=12 method=POST path=/echo query=code=***&size=20 status=200"
        );
        assertThat(output).doesNotContain("apple-id-token", "response-access-token", "apple-code");
    }

    @Test
    @DisplayName("requestId가 없으면 새로 생성해 응답 header와 로그에 사용한다")
    void adds_generated_request_id_when_missing(CapturedOutput output) throws Exception {
        // when & then
        mockMvc.perform(get("/public"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));

        assertThat(output).contains("event=http_request");
        assertThat(output).contains("userId=anonymous");
        assertThat(output).contains("method=GET path=/public query= status=200");
    }

    @Test
    @DisplayName("Authorization header 형식이 잘못되어도 요청을 막지 않고 anonymous 로그를 남긴다")
    void keeps_request_successful_and_logs_anonymous_when_authorization_header_is_malformed(CapturedOutput output) throws Exception {
        // when & then
        mockMvc.perform(get("/public")
                        .header(HttpHeaders.AUTHORIZATION, "Basic invalid-token"))
                .andExpect(status().isOk());

        assertThat(output).contains("event=http_request");
        assertThat(output).contains("userId=anonymous");
        assertThat(output).contains("method=GET path=/public query= status=200");
    }

    @Test
    @DisplayName("multipart 요청 body는 로그로 남기지 않고 응답 body는 유지한다")
    void preserves_response_body_without_logging_multipart_request_body(CapturedOutput output) throws Exception {
        // when & then
        mockMvc.perform(multipart("/upload")
                        .file("file", "file-content".getBytes()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "objectKey": "activity-record-image/12/file.png"
                        }
                        """));

        assertThat(output).doesNotContain("event=http_request_body");
        assertThat(output).contains("event=http_response_body");
        assertThat(output).contains("event=http_request");
    }

    @Test
    @DisplayName("API 로그 생성이 실패해도 실제 응답 body는 유지한다")
    void preserves_response_body_when_api_logging_fails() throws Exception {
        // given
        ApiLogWriter failingLogWriter = new ApiLogWriter() {
            @Override
            public void httpResponseBody(ApiLogContext context, int status, String body) {
                throw new IllegalStateException("logging failed");
            }
        };
        MockMvc mockMvcWithFailingLogger = standaloneSetup(new LoggingTestController())
                .addFilters(new RequestLoggingFilter(
                        jwtTokenProvider,
                        new HttpBodySanitizer(new ObjectMapper()),
                        new QueryStringSanitizer(),
                        failingLogWriter
                ))
                .build();

        // when & then
        mockMvcWithFailingLogger.perform(get("/public"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "ok": true
                        }
                        """));
    }

    @Test
    @DisplayName("4KB를 넘는 JSON 요청 body도 파싱 불가 로그로 떨어지지 않고 제한 길이로 남긴다")
    void logs_truncated_json_body_when_request_body_is_larger_than_four_kilobytes(CapturedOutput output) throws Exception {
        // given
        String longMemo = "a".repeat(5_000);

        // when & then
        mockMvc.perform(post("/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityToken": "apple-id-token",
                                  "memo": "%s"
                                }
                                """.formatted(longMemo)))
                .andExpect(status().isOk());

        assertThat(output).contains(
                "event=http_request_body",
                "\"identityToken\":\"***\"",
                "...[truncated]"
        );
        assertThat(output).doesNotContain("[unparseable-json]", "apple-id-token");
    }

    @Test
    @DisplayName("제외 path는 API 로그를 남기지 않는다")
    void skips_api_logs_for_excluded_paths(CapturedOutput output) throws Exception {
        // when & then
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        assertThat(output).doesNotContain("event=http_request");
        assertThat(output).doesNotContain("event=http_request_body");
        assertThat(output).doesNotContain("event=http_response_body");
    }

    @RestController
    static class LoggingTestController {

        @PostMapping(value = "/echo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> echo(@RequestBody Map<String, Object> request) {
            return Map.of(
                    "accessToken", "response-access-token",
                    "memo", request.get("memo")
            );
        }

        @GetMapping(value = "/public", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> publicEndpoint() {
            return Map.of("ok", true);
        }

        @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> upload() {
            return Map.of("objectKey", "activity-record-image/12/file.png");
        }

        @GetMapping(value = "/actuator/health", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> health() {
            return Map.of("status", "UP");
        }

        @GetMapping(value = "/actuator", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> actuator() {
            return Map.of("status", "UP");
        }
    }
}
