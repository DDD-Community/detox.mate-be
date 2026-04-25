package com.detoxmate.notification.listener;

import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.event.CertificationCreatedEvent;
import com.detoxmate.notification.event.CommentCreatedEvent;
import com.detoxmate.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CertificationCreatedEvent event) {
        notificationService.send(
                event.recipientUserId(),
                NotificationTypeCode.CERTIFICATION,
                event.actorName()
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentCreatedEvent event) {
        notificationService.send(
                event.recipientUserId(),
                NotificationTypeCode.COMMENT,
                event.actorNickname()
        );
    }
}
