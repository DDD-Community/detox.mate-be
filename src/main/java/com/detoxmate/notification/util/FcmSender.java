package com.detoxmate.notification.util;

import java.util.Map;

public interface FcmSender {
    void send(String token, String title, String body, Map<String, String> data);
}
