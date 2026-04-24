package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;

import java.util.HashMap;
import java.util.Map;

/**
 * 혹시 몰라 작성한 VO입니다.
 * 댓글 이벤트에 댓글까지 들어가게 되면 이 Context를 사용해서 받을 예정입니다.
 */
public record NotificationContext(Map<String,String> variables) {
    public NotificationContext {
        variables = Map.copyOf(variables);
    }

    public static NotificationContext of(String... kv) {
        if (kv.length%2!=0) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_CONTEXT_INVALID_PAIRS);
        }
        Map<String, String> map = new HashMap<>();

        for (int i=0; i<kv.length; i+=2) {
            map.put(kv[i], kv[i+1]);
        }
        return new NotificationContext(map);
    }

    public String get(String key) {
        return variables.get(key);
    }
}
