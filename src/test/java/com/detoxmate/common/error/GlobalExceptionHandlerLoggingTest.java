package com.detoxmate.common.error;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.ErrorCode;
import com.detoxmate.common.exception.user.UserCodeException;
import com.detoxmate.common.logging.ApiLogWriter;
import com.detoxmate.common.logging.HttpBodySanitizer;
import com.detoxmate.common.logging.QueryStringSanitizer;
import com.detoxmate.common.logging.RequestLoggingFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("GlobalExceptionHandler logging")
class GlobalExceptionHandlerLoggingTest {

    private static final String JWT_SECRET = "this-is-a-very-long-secret-key-for-exception-logging-test";

    private final RequestLoggingFilter filter = new RequestLoggingFilter(
            new JwtTokenProvider(JWT_SECRET, 3600L),
            new HttpBodySanitizer(),
            new QueryStringSanitizer(),
            new ApiLogWriter()
    );
    private final MockMvc mockMvc = standaloneSetup(new ExceptionTestController())
            .setControllerAdvice(GlobalExceptionHandlerTestFixture.globalExceptionHandler())
            .addFilters(filter)
            .build();

    @Test
    @DisplayName("예상 가능한 ResponseStatusException은 handled error를 INFO로 남긴다")
    void logs_handled_error_at_info_when_response_status_exception_is_expected(CapturedOutput output) throws Exception {
        // when & then
        mockMvc.perform(get("/handled")
                        .header("X-Request-Id", "req-handled"))
                .andExpect(status().isBadRequest());

        assertThat(output).contains(
                "INFO",
                "event=api_handled_error requestId=req-handled userId=anonymous method=GET path=/handled status=400 errorCode=INVALID_REQUEST"
        );
        assertThat(output).doesNotContain("event=api_error");
    }

    @Test
    @DisplayName("비즈니스 CustomException은 handled error를 INFO로 남긴다")
    void logs_handled_error_at_info_when_custom_exception_is_expected(CapturedOutput output) throws Exception {
        // when & then
        mockMvc.perform(post("/business")
                        .header("X-Request-Id", "req-business"))
                .andExpect(status().isConflict());

        assertThat(output).contains(
                "INFO",
                "event=api_handled_error requestId=req-business userId=anonymous method=POST path=/business status=409 errorCode=ALREADY_EXISTS"
        );
        assertThat(output).doesNotContain("event=api_error");
    }

    @Test
    @DisplayName("잘못된 사용자 코드는 400 응답과 HTTP 로그를 유지하고 오류 이벤트는 남기지 않는다")
    void preserves_http_response_and_logs_without_error_events_when_user_code_is_invalid(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/invalid-user-code")
                        .header("X-Request-Id", "req-invalid-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER_CODE"))
                .andExpect(jsonPath("$.message").value("올바른 사용자 코드가 아닙니다."))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(output).contains(
                "event=http_response_body requestId=req-invalid-code userId=anonymous method=GET path=/invalid-user-code status=400",
                "event=http_request requestId=req-invalid-code userId=anonymous method=GET path=/invalid-user-code"
        );
        assertThat(output).doesNotContain("event=api_handled_error", "event=api_error");
    }

    @Test
    @DisplayName("사용자 코드 누락은 기존 400 응답과 handled error 기록을 유지한다")
    void logs_handled_error_when_user_code_is_required(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/required-user-code")
                        .header("X-Request-Id", "req-required-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_CODE_REQUIRED"))
                .andExpect(jsonPath("$.message").value("사용자 코드를 입력해 주세요."))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(output).contains(
                "event=api_handled_error requestId=req-required-code userId=anonymous method=GET path=/required-user-code status=400 errorCode=USER_CODE_REQUIRED"
        );
        assertThat(output).doesNotContain("event=api_error");
    }

    @Test
    @DisplayName("예상하지 못한 예외는 api_error와 stack trace를 ERROR로 남긴다")
    void logs_api_error_with_stack_trace_when_exception_is_unexpected(CapturedOutput output) throws Exception {
        // when & then
        mockMvc.perform(get("/server-error")
                        .header("X-Request-Id", "req-error"))
                .andExpect(status().isInternalServerError());

        assertThat(output).contains(
                "ERROR",
                "event=api_error requestId=req-error userId=anonymous method=GET path=/server-error status=500 exception=IllegalStateException fingerprint=GET:/server-error:IllegalStateException",
                "java.lang.IllegalStateException: boom"
        );
    }

    @Test
    @DisplayName("존재하지 않는 경로 예외는 404 handled error로 남긴다")
    void logs_handled_error_at_info_when_resource_is_not_found(CapturedOutput output) throws Exception {
        // when & then
        mockMvc.perform(get("/missing-resource")
                        .header("X-Request-Id", "req-not-found"))
                .andExpect(status().isNotFound());

        assertThat(output).contains(
                "INFO",
                "event=api_handled_error requestId=req-not-found userId=anonymous method=GET path=/missing-resource status=404 errorCode=NOT_FOUND"
        );
        assertThat(output).doesNotContain("event=api_error");
    }

    @RestController
    static class ExceptionTestController {

        @GetMapping("/handled")
        void handled() {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad request");
        }

        @PostMapping("/business")
        void business() {
            throw new CustomException(TestErrorCode.ALREADY_EXISTS);
        }

        @GetMapping(value = "/server-error", produces = MediaType.APPLICATION_JSON_VALUE)
        void serverError() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/invalid-user-code")
        void invalidUserCode() {
            throw new CustomException(UserCodeException.INVALID_USER_CODE);
        }

        @GetMapping("/required-user-code")
        void requiredUserCode() {
            throw new CustomException(UserCodeException.USER_CODE_REQUIRED);
        }

        @GetMapping("/missing-resource")
        void missingResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "/", "No static resource.");
        }
    }

    enum TestErrorCode implements ErrorCode {
        ALREADY_EXISTS(HttpStatus.CONFLICT, "Already exists");

        private final HttpStatus httpStatus;
        private final String message;

        TestErrorCode(HttpStatus httpStatus, String message) {
            this.httpStatus = httpStatus;
            this.message = message;
        }

        @Override
        public HttpStatus getHttpStatus() {
            return httpStatus;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
