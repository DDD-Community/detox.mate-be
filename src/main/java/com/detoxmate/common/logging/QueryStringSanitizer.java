package com.detoxmate.common.logging;

import org.springframework.stereotype.Component;

@Component
public class QueryStringSanitizer {

    static final int MAX_QUERY_STRING_LENGTH = 1024;
    private static final String MASKED_VALUE = "***";
    private static final String TRUNCATED_SUFFIX = "...[truncated]";

    public String sanitize(String rawQueryString) {
        if (rawQueryString == null || rawQueryString.isBlank()) {
            return "";
        }

        String[] parameters = rawQueryString.split("&", -1);
        StringBuilder sanitized = new StringBuilder();

        for (int index = 0; index < parameters.length; index++) {
            if (index > 0) {
                sanitized.append("&");
            }
            sanitized.append(sanitizeParameter(parameters[index]));
        }

        return truncateQueryString(sanitized.toString());
    }

    private String sanitizeParameter(String parameter) {
        int separatorIndex = parameter.indexOf('=');
        if (separatorIndex < 0) {
            return SensitiveLogKeywordMatcher.matches(parameter) ? parameter + "=" + MASKED_VALUE : parameter;
        }

        String name = parameter.substring(0, separatorIndex);
        if (SensitiveLogKeywordMatcher.matches(name)) {
            return name + "=" + MASKED_VALUE;
        }

        return parameter;
    }

    private String truncateQueryString(String queryString) {
        if (queryString.length() <= MAX_QUERY_STRING_LENGTH) {
            return queryString;
        }

        return queryString.substring(0, MAX_QUERY_STRING_LENGTH) + TRUNCATED_SUFFIX;
    }
}
