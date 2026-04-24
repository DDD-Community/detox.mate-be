package com.detoxmate.notification.listener;

import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.event.CertificationCreatedEvent;
import com.detoxmate.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
class NotificationEventListenerTest {

    @Autowired
    ApplicationEventPublisher publisher;
    @Autowired
    TransactionTemplate transactionTemplate;

    @MockitoBean
    NotificationService notificationService;

    @TestConfiguration
    static class SyncExecutorConfig{
        @Bean@Primary
        public Executor notificationTaskExecutor(){
            return Runnable::run;
        }
    }

    @Test
    @DisplayName("CertificationCreatedEvent 발행 후 커밋되면 send(CERTIFICATION)이 호출된다")
    void publishesCertificationNotification_afterCommit() {
        //given
        Long userId = 1L;
        String nickname = "xeulbn";

        //when
        transactionTemplate.executeWithoutResult(status ->
                publisher.publishEvent(new CertificationCreatedEvent(userId, nickname))
        );

        //then
        verify(notificationService).send(userId, NotificationTypeCode.CERTIFICATION, nickname);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 send()가 호출되지 않는다")
    void doesNotSend_whenTransactionRollsBack() {
        //given
        transactionTemplate.execute(status -> {
            publisher.publishEvent(new CertificationCreatedEvent(1L, "abc"));
            status.setRollbackOnly();
            return null;
        });

        //when & then
        verifyNoInteractions(notificationService);
    }

}