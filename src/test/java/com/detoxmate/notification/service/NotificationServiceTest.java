package com.detoxmate.notification.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.FcmSenderErrorCode;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import com.detoxmate.notification.domain.*;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.notification.repository.NotificationRepository;
import com.detoxmate.notification.repository.NotificationTypeRepository;
import com.detoxmate.notification.util.FcmSender;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    private static final String DEFAULT_TITLE = "인증 시간 알림";
    private static final String DEFAULT_TEMPLATE = "{nickname}님, 오늘 인증까지 1시간 남았습니다.";

    @Autowired
    NotificationService notificationService;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    FcmTokenRepository fcmTokenRepository;
    @Autowired
    NotificationHistoryRepository historyRepository;
    @Autowired
    NotificationTypeRepository notificationTypeRepository;
    @Autowired
    UserRepository userRepository;

    @MockitoBean
    FcmSender fcmSender;

    private NotificationType certificationType;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        fcmTokenRepository.deleteAll();
        notificationRepository.deleteAll();
        notificationTypeRepository.deleteAll();

        certificationType = notificationTypeRepository.save(
                NotificationType.create(NotificationTypeCode.CERTIFICATION_CREATED)
        );
    }


    @Test
    @DisplayName("사용자에게 알림을 전송하면 FCM 푸시가 발송되고 이력이 저장된다")
    void sendNotification_toSingleDevice() {
        // given
        User user = saveUser("xeulbn");
        User sender = saveUser("sender");
        saveDefaultNotification();
        saveToken(user.getId(), "test-token-abc", DevicePlatform.IOS);

        // when
        notificationService.send(defaultCommand(user.getId(), sender.getId()));

        // then
        assertThat(historyRepository.findAll())
                .extracting(NotificationHistory::getMessage)
                .containsExactly("xeulbn님, 오늘 인증까지 1시간 남았습니다.");
        assertThat(historyRepository.findAll())
                .extracting(NotificationHistory::getSenderUserId)
                .containsExactly(sender.getId());
        verify(fcmSender).send(
                eq("test-token-abc"),
                eq(DEFAULT_TITLE),
                eq("xeulbn님, 오늘 인증까지 1시간 남았습니다."),
                eq(NotificationPayload.none().toFcmData(NotificationTypeCode.CERTIFICATION_CREATED))
        );
    }

    @Test
    @DisplayName("사용자의 여러 디바이스에 각각 FCM 푸시가 전송된다")
    void sendNotification_toMultipleDevices() {
        //given
        User user = saveUser("xeulbn");
        saveDefaultNotification();
        saveToken(user.getId(), "test-token-abc-IOS", DevicePlatform.IOS);
        saveToken(user.getId(), "test-token-abc-AND", DevicePlatform.ANDROID);

        //when
        notificationService.send(defaultCommand(user.getId()));

        //then
        verify(fcmSender).send(eq("test-token-abc-IOS"), anyString(), anyString(), anyMap());
        verify(fcmSender).send(eq("test-token-abc-AND"), anyString(), anyString(), anyMap());
        verify(fcmSender, times(2)).send(anyString(), anyString(), anyString(), anyMap());
        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("토큰이 없는 사용자에게 전송하면 FCM 호출은 없지만 이력은 저장된다")
    void sendNotification_noTokens() {
        //given
        User user = saveUser("xeulbn");
        saveDefaultNotification();

        //when
        notificationService.send(defaultCommand(user.getId()));

        //then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 타입의 알림을 전송하면 예외가 발생한다")
    void sendNotification_templateNotFound() {
        // given
        User user = saveUser("xeulbn");
        saveToken(user.getId(), "test-token-abc", DevicePlatform.IOS);

        //when
        assertThatThrownBy(() -> notificationService.send(defaultCommand(user.getId())))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);

        //then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
        assertThat(historyRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("일부 FCM 전송이 실패해도 나머지 디바이스로는 계속 전송된다")
    void sendNotification_partialFailure_continues() {
        //given
        User user = saveUser("xeulbn");
        saveDefaultNotification();
        saveToken(user.getId(), "token-fail", DevicePlatform.IOS);
        saveToken(user.getId(), "token-ok", DevicePlatform.ANDROID);
        doThrow(new CustomException(FcmSenderErrorCode.FCM_SEND_FAILED))
                .when(fcmSender).send(eq("token-fail"), anyString(), anyString(), anyMap());

        //when
        notificationService.send(defaultCommand(user.getId()));

        //then
        verify(fcmSender).send(eq("token-ok"), anyString(), anyString(), anyMap());
        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("FCM이 UNREGISTERED 응답을 주면 해당 토큰은 DB에서 삭제된다")
    void deadTokenCleanup_unregistered() {
        // given
        User user = saveUser("xeulbn");
        saveDefaultNotification();
        fcmTokenRepository.save(FcmToken.create(user.getId(), "dead-token", DevicePlatform.ANDROID));
        fcmTokenRepository.save(FcmToken.create(user.getId(), "alive-token", DevicePlatform.IOS));

        // dead-token은 UNREGISTERED 에러
        doThrow(new CustomException(FcmSenderErrorCode.FCM_TOKEN_UNREGISTERED))
                .when(fcmSender).send(eq("dead-token"), anyString(), anyString(), anyMap());
        // alive-token은 성공

        // when
        notificationService.send(defaultCommand(user.getId()));

        // then — 죽은 토큰은 삭제
        assertThat(fcmTokenRepository.findByToken("dead-token")).isEmpty();
        // then — 살아있는 토큰은 유지
        assertThat(fcmTokenRepository.findByToken("alive-token")).isPresent();
    }

    @Test
    @DisplayName("일시적 네트워크 오류(FCM_SEND_FAILED)는 토큰을 삭제하지 않는다")
    void deadTokenCleanup_preservesOnTemporaryFailure() {
        // given
        User user = saveUser("xeulbn");
        saveDefaultNotification();
        fcmTokenRepository.save(FcmToken.create(user.getId(), "test-token", DevicePlatform.ANDROID));

        doThrow(new CustomException(FcmSenderErrorCode.FCM_SEND_FAILED))
                .when(fcmSender).send(eq("test-token"), anyString(), anyString(), anyMap());

        // when
        notificationService.send(defaultCommand(user.getId()));

        // then — 일시 오류니까 보존
        assertThat(fcmTokenRepository.findByToken("test-token")).isPresent();
    }

    @Test
    @DisplayName("알림 수신을 거부한 사용자에게는 이력만 저장하고 FCM 푸시는 전송하지 않는다")
    void sendNotification_pushDisabled_savesHistoryOnly() {
        // given
        User user = saveUser("xeulbn");
        user.updatePushNotificationEnabled(false);
        saveDefaultNotification();
        saveToken(user.getId(), "test-token-abc", DevicePlatform.IOS);

        // when
        notificationService.send(defaultCommand(user.getId()));

        // then
        assertThat(historyRepository.findAll())
                .extracting(NotificationHistory::getMessage)
                .containsExactly("xeulbn님, 오늘 인증까지 1시간 남았습니다.");
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
        assertThat(fcmTokenRepository.findByToken("test-token-abc")).isPresent();
    }

    private Notification saveDefaultNotification() {
        return notificationRepository.save(
                Notification.create(certificationType, DEFAULT_TITLE, DEFAULT_TEMPLATE)
        );
    }

    private FcmToken saveToken(Long userId, String token, DevicePlatform platform) {
        return fcmTokenRepository.save(FcmToken.create(userId, token, platform));
    }

    private User saveUser(String displayName) {
        return userRepository.save(User.createNew(displayName));
    }

    private NotificationCommand defaultCommand(Long userId) {
        return defaultCommand(userId, null);
    }

    private NotificationCommand defaultCommand(Long userId, Long senderUserId) {
        return NotificationCommand.history(
                userId,
                senderUserId,
                NotificationTypeCode.CERTIFICATION_CREATED,
                NotificationContext.of("nickname", "xeulbn"),
                NotificationPayload.none()
        );
    }
}
