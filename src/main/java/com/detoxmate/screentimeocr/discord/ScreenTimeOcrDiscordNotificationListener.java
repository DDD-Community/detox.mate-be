package com.detoxmate.screentimeocr.discord;

import com.detoxmate.screentimeocr.event.ScreenTimeOcrErrorReportCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScreenTimeOcrDiscordNotificationListener {

    private final ScreenTimeOcrDiscordNotifier notifier;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ScreenTimeOcrErrorReportCreatedEvent event) {
        try {
            notifier.send(event.reportId());
        } catch (RuntimeException exception) {
            log.warn("Failed to send screen time OCR report Discord notification. reportId={}", event.reportId(), exception);
        }
    }
}
