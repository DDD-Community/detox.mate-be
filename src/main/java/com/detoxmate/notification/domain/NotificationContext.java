package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;

import java.util.HashMap;
import java.util.Map;

public record NotificationContext(Map<String,String> variables) {
    public NotificationContext {
        variables = Map.copyOf(variables);
    }

    public static NotificationContext empty() {
        return new NotificationContext(Map.of());
    }

    public static NotificationContext of(String... kv) {
        if (kv.length%2 != 0) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_CONTEXT_INVALID_PAIRS);
        }

        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }

        return new NotificationContext(map);
    }

    public String get(String key) {
        return variables.get(key);
    }
}
