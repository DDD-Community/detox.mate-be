package com.detoxmate.notification.util;

public interface FcmSender {
    void send(String token, String title, String body);
}
