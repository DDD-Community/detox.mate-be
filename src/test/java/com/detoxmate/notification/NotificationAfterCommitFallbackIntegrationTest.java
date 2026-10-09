package com.detoxmate.notification;

import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.repository.FriendRepository;
import com.detoxmate.friend.service.FriendService;
import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.notification.util.FcmSender;
import com.detoxmate.support.UserFixtures;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationAfterCommitFallbackIntegrationTest {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:notification-after-commit-fallback;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    }

    @Autowired private UserRepository userRepository;
    @Autowired private FriendRepository friendRepository;
    @Autowired private FriendService friendService;
    @Autowired private FcmTokenRepository tokenRepository;
    @Autowired private NotificationHistoryRepository historyRepository;
    @Autowired private RecordingFcmSender fcm;

    private User requester;
    private User receiver;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        friendRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        fcm.messages.clear();
        // Each repository operation commits; there is no surrounding test transaction.
        requester = createUser("요청친구");
        receiver = createUser("받는친구");
    }

    @Test
    @DisplayName("친구 요청 알림이 호출자 스레드에서 발송되어도 관계와 알림 이력을 모두 저장한다")
    void sendFriendRequest_callerThreadFallbackPersistsNotificationHistory() {
        // when
        friendService.sendRequest(requester.getId(), receiver.getId());

        // then
        assertThat(friendRepository.findByUserPair(requester.getId(), receiver.getId())).get()
                .satisfies(friend -> assertThat(friend.isPending()).isTrue());
        assertPushAndPersistedHistory(receiver, requester,
                "요청친구님이 친구 요청을 보냈어요.", "FRIEND_REQUESTS");
    }

    @Test
    @DisplayName("친구 수락 알림이 호출자 스레드에서 발송되어도 수락 상태와 알림 이력을 모두 저장한다")
    void acceptFriendRequest_callerThreadFallbackPersistsNotificationHistory() {
        // given
        Friend pending = friendRepository.saveAndFlush(Friend.request(requester.getId(), receiver.getId()));

        // when
        friendService.acceptRequest(pending.getId(), receiver.getId());

        // then
        assertThat(friendRepository.findById(pending.getId())).get()
                .satisfies(friend -> assertThat(friend.isAccepted()).isTrue());
        assertPushAndPersistedHistory(requester, receiver,
                "받는친구님과 친구가 되었어요! 함께 스크린타임을 줄여봐요.", "FRIENDS");
    }

    private void assertPushAndPersistedHistory(User recipient, User sender, String body, String target) {
        assertThat(fcm.messages).singleElement().satisfies(push -> {
            assertThat(push.token()).isEqualTo(token(recipient));
            assertThat(push.title()).isEqualTo("Detoxmate");
            assertThat(push.body()).isEqualTo(body);
            assertThat(push.data()).containsEntry("targetType", target);
        });
        // A fresh repository transaction must observe history after the friend transaction returns.
        assertThat(historyRepository.findAll()).singleElement().satisfies(history -> {
            assertThat(history.getUserId()).isEqualTo(recipient.getId());
            assertThat(history.getSenderUserId()).isEqualTo(sender.getId());
            assertThat(history.getTitle()).isEqualTo("Detoxmate");
            assertThat(history.getMessage()).isEqualTo(body);
            assertThat(history.getTargetType().name()).isEqualTo(target);
        });
        assertThat(fcm.messages).allSatisfy(push -> assertThat(push.transactionActive()).isFalse());
    }

    private User createUser(String name) {
        User user = userRepository.saveAndFlush(UserFixtures.createUser(name));
        tokenRepository.saveAndFlush(FcmToken.create(user.getId(), token(user), DevicePlatform.IOS));
        return user;
    }

    private String token(User user) {
        return "after-commit-fallback-token-" + user.getId();
    }

    record SentPush(String token, String title, String body, Map<String, String> data,
                    boolean transactionActive) { }

    static class RecordingFcmSender implements FcmSender {
        final List<SentPush> messages = new CopyOnWriteArrayList<>();

        @Override
        public void send(String token, String title, String body, Map<String, String> data) {
            messages.add(new SentPush(token, title, body, Map.copyOf(data),
                    TransactionSynchronizationManager.isActualTransactionActive()));
        }
    }

    @TestConfiguration
    static class CallerThreadDeliveryConfiguration {
        @Bean
        @Primary
        RecordingFcmSender recordingFcmSender() {
            return new RecordingFcmSender();
        }

        @Bean(name = "notificationTaskExecutor")
        Executor notificationTaskExecutor() {
            // Deterministically exercise the production executor's CallerRunsPolicy fallback.
            return Runnable::run;
        }
    }
}
