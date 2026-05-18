package com.detoxmate.screentimeocr.discord;

import com.detoxmate.screentimeocr.event.ScreenTimeOcrErrorReportCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Executor;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
class ScreenTimeOcrDiscordNotificationListenerTest {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TransactionTemplate transactionTemplate;

    @MockitoBean
    ScreenTimeOcrDiscordNotifier notifier;

    @TestConfiguration
    static class SyncExecutorConfig {
        @Bean
        @Primary
        public Executor notificationTaskExecutor() {
            return Runnable::run;
        }
    }

    @Test
    void OCR_오류_신고_생성_이벤트가_커밋되면_Discord_알림을_보낸다() {
        transactionTemplate.executeWithoutResult(status ->
                publisher.publishEvent(new ScreenTimeOcrErrorReportCreatedEvent(555L))
        );

        verify(notifier).send(555L);
    }

    @Test
    void 트랜잭션이_롤백되면_Discord_알림을_보내지_않는다() {
        transactionTemplate.execute(status -> {
            publisher.publishEvent(new ScreenTimeOcrErrorReportCreatedEvent(555L));
            status.setRollbackOnly();
            return null;
        });

        verifyNoInteractions(notifier);
    }

    @Test
    void Discord_알림_실패는_이벤트_처리_밖으로_전파하지_않는다() {
        doThrow(new IllegalStateException("discord failed")).when(notifier).send(555L);

        transactionTemplate.executeWithoutResult(status ->
                publisher.publishEvent(new ScreenTimeOcrErrorReportCreatedEvent(555L))
        );

        verify(notifier).send(555L);
    }
}
