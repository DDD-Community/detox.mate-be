package com.detoxmate.notification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@Profile("test")
public class TestFcmSender implements FcmSender {

    @Override
    public void send(String token, String title, String body, Map<String, String> data) {
        log.debug("[Test] FCM send: token={}, title={}, body={}, data={}",
                mask(token), title, body, data);
    }
    private String mask(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
