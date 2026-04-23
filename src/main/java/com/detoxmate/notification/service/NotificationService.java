package com.detoxmate.notification.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.ErrorCode;
import com.detoxmate.common.exception.notification.FcmSenderErrorCode;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.domain.Notification;
import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.notification.repository.NotificationRepository;
import com.detoxmate.notification.util.FcmSender;
import com.detoxmate.notification.util.TokenMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationHistoryRepository historyRepository;
    private final FcmSender fcmSender;
    private final TransactionTemplate transactionTemplate;

    public void send(Long userId, NotificationTypeCode typeCode, String nickname){
        DispatchContext context = prepareWithinTx(userId,typeCode,nickname);
        List<String> deadTokens = dispatchPush(context,userId);
        cleanUpDeadTokens(deadTokens);

        log.info("[Notification][send] done. userId={}, typeCode={}, sentTokens={}, deadTokens={}",
                userId, typeCode,
                context.tokens().size() - deadTokens.size(),
                deadTokens.size());
    }

    private DispatchContext prepareWithinTx(Long userId, NotificationTypeCode typeCode, String nickname){
        return transactionTemplate.execute(status ->{
            Notification notification = notificationRepository.findByTypeCode(typeCode)
                    .orElseThrow(()-> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

            List<FcmToken> tokens = fcmTokenRepository.findAllByUserId(userId);
            historyRepository.save(NotificationHistory.from(notification,userId,nickname));

            return new DispatchContext(
                    tokens.stream().map(FcmToken::getToken).toList(),
                    notification.getTitle(),
                    notification.resolve(nickname)
            );
        });
    }

    private List<String> dispatchPush(DispatchContext context,Long userId){
        List<String> deadTokens = new ArrayList<>();

        for(String token : context.tokens()){
            try{
                fcmSender.send(token, context.title(),context.body());
            }catch(CustomException e){
                log.warn("[Notification][dispatch-push] FCM send failed. userId={},token={},errorCode={}",
                        userId, TokenMasker.mask(token),e.getErrorCode(),e);

                if(isDeadTokenError(e.getErrorCode())){
                    deadTokens.add(token);
                }
            }
        }
        return deadTokens;
    }

    private boolean isDeadTokenError(ErrorCode errorCode){
        return errorCode== FcmSenderErrorCode.FCM_TOKEN_UNREGISTERED||
                errorCode==FcmSenderErrorCode.FCM_INVALID_TOKEN;
    }

    private void cleanUpDeadTokens(List<String> tokens){
        if(tokens.isEmpty()){
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            tokens.forEach(fcmTokenRepository::deleteByToken);
        });
        log.info("[Notification][clean-up-dead-tokens] Cleaned up {} dead FCM tokens",tokens.size());
    }

    private record DispatchContext(List<String> tokens, String title, String body) {}
}
