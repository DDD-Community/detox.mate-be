package com.detoxmate.common.logging;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.common.AccessTokenExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String ANONYMOUS_USER = "anonymous";
    private static final int BODY_CACHE_LIMIT = 64 * 1024;
    private static final List<String> EXCLUDED_PATH_ROOTS = List.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );
    private static final List<String> EXCLUDED_EXACT_PATHS = List.of(
            "/openapi3.yaml",
            "/favicon.ico"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final HttpBodySanitizer httpBodySanitizer;
    private final QueryStringSanitizer queryStringSanitizer;
    private final ApiLogWriter apiLogWriter;

    public RequestLoggingFilter(
            JwtTokenProvider jwtTokenProvider,
            HttpBodySanitizer httpBodySanitizer,
            QueryStringSanitizer queryStringSanitizer,
            ApiLogWriter apiLogWriter
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.httpBodySanitizer = httpBodySanitizer;
        this.queryStringSanitizer = queryStringSanitizer;
        this.apiLogWriter = apiLogWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isExcludedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestId = getOrCreateRequestId(request);
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, BODY_CACHE_LIMIT);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        long startedAt = System.currentTimeMillis();
        String userId = getUserId(wrappedRequest);

        MDC.put(REQUEST_ID, requestId);
        MDC.put(USER_ID, userId);
        wrappedResponse.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            try {
                logRequestAndResponse(wrappedRequest, wrappedResponse, requestId, userId, startedAt);
            } catch (RuntimeException exception) {
                apiLogWriter.apiLoggingFailed(toApiLogContext(wrappedRequest, requestId, userId), exception);
            } finally {
                MDC.remove(REQUEST_ID);
                MDC.remove(USER_ID);
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    private void logRequestAndResponse(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            String requestId,
            String userId,
            long startedAt
    ) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();
        ApiLogContext context = new ApiLogContext(requestId, userId, method, path);

        if (hasJsonRequestBody(request)) {
            apiLogWriter.httpRequestBody(context, sanitizeRequestBody(request));
        }

        if (hasJsonResponseBody(response)) {
            apiLogWriter.httpResponseBody(context, status, sanitizeResponseBody(response));
        }

        apiLogWriter.httpRequest(
                context,
                queryStringSanitizer.sanitize(request.getQueryString()),
                status,
                System.currentTimeMillis() - startedAt);
    }

    private String getOrCreateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }

        return UUID.randomUUID().toString();
    }

    private String getUserId(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            String accessToken = AccessTokenExtractor.require(authorizationHeader);
            return String.valueOf(jwtTokenProvider.getUserId(accessToken));
        } catch (RuntimeException exception) {
            return ANONYMOUS_USER;
        }
    }

    private boolean hasJsonRequestBody(ContentCachingRequestWrapper request) {
        return isJsonContentType(request.getContentType())
                && request.getContentAsByteArray().length > 0;
    }

    private boolean hasJsonResponseBody(ContentCachingResponseWrapper response) {
        return isJsonContentType(response.getContentType())
                && response.getContentAsByteArray().length > 0;
    }

    private boolean isJsonContentType(String contentType) {
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private String sanitizeRequestBody(ContentCachingRequestWrapper request) {
        return httpBodySanitizer.sanitize(new String(request.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private String sanitizeResponseBody(ContentCachingResponseWrapper response) {
        return httpBodySanitizer.sanitize(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private ApiLogContext toApiLogContext(HttpServletRequest request, String requestId, String userId) {
        return new ApiLogContext(requestId, userId, request.getMethod(), request.getRequestURI());
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_EXACT_PATHS.contains(path)
                || EXCLUDED_PATH_ROOTS.stream()
                .anyMatch(excludedPathRoot -> path.equals(excludedPathRoot) || path.startsWith(excludedPathRoot + "/"));
    }
}
