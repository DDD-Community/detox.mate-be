package com.detoxmate.notification.util;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.FcmSenderErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class FirebaseFcmSender implements FcmSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void send(String token, String title, String body) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try{
            String response = firebaseMessaging.send(message);
            log.info("[Notification][FirebaseFcmSender] FCM message sent successfully. token = {}, response={}",mask(token),response);
        }catch(FirebaseMessagingException e){
            log.error("[Notification][FirebaseFcmSender]FCM send failed. token = {}, errorCode={}",mask(token),e.getMessagingErrorCode());
            throw new CustomException(resolveErrorCode(e));
        }
    }

    private String mask(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private FcmSenderErrorCode resolveErrorCode(FirebaseMessagingException e) {
        return switch (e.getMessagingErrorCode()) {
            case UNREGISTERED -> FcmSenderErrorCode.FCM_TOKEN_UNREGISTERED;
            case INVALID_ARGUMENT -> FcmSenderErrorCode.FCM_INVALID_TOKEN;
            default -> FcmSenderErrorCode.FCM_SEND_FAILED;
        };
    }
}
