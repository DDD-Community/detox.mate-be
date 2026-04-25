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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @MockitoBean
    FcmSender fcmSender;

    private NotificationType certificationType;

    @BeforeEach
    void setUp() {
        certificationType = notificationTypeRepository.save(
                NotificationType.create(NotificationTypeCode.CERTIFICATION)
        );
    }


    @Test
    @DisplayName("사용자에게 알림을 전송하면 FCM 푸시가 발송되고 이력이 저장된다")
    void sendNotification_toSingleDevice() {
        // given
        saveDefaultNotification();
        saveToken(1L, "test-token-abc", DevicePlatform.IOS);

        // when
        notificationService.send(1L, NotificationTypeCode.CERTIFICATION, "xeulbn");

        // then
        assertThat(historyRepository.findAll()).hasSize(1);
        verify(fcmSender).send(
                eq("test-token-abc"),
                eq(DEFAULT_TITLE),
                eq("xeulbn님, 오늘 인증까지 1시간 남았습니다.")
        );
    }

    @Test
    @DisplayName("사용자의 여러 디바이스에 각각 FCM 푸시가 전송된다")
    void sendNotification_toMultipleDevices() {
        //given
        saveDefaultNotification();
        saveToken(1L, "test-token-abc-IOS", DevicePlatform.IOS);
        saveToken(1L, "test-token-abc-AND", DevicePlatform.ANDROID);

        //when
        notificationService.send(1L, NotificationTypeCode.CERTIFICATION, "xeulbn");

        //then
        verify(fcmSender).send(eq("test-token-abc-IOS"), anyString(), anyString());
        verify(fcmSender).send(eq("test-token-abc-AND"), anyString(), anyString());
        verify(fcmSender, times(2)).send(anyString(), anyString(), anyString());
        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("토큰이 없는 사용자에게 전송하면 FCM 호출은 없지만 이력은 저장된다")
    void sendNotification_noTokens() {
        //given
        saveDefaultNotification();

        //when
        notificationService.send(999L, NotificationTypeCode.CERTIFICATION, "xeulbn");

        //then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString());
        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 타입의 알림을 전송하면 예외가 발생한다")
    void sendNotification_templateNotFound() {
        // given
        saveToken(1L, "test-token-abc", DevicePlatform.IOS);

        //when
        assertThatThrownBy(() -> notificationService.send(1L, NotificationTypeCode.CERTIFICATION, "xeulbn"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);

        //then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString());
        assertThat(historyRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("일부 FCM 전송이 실패해도 나머지 디바이스로는 계속 전송된다")
    void sendNotification_partialFailure_continues() {
        //given
        saveDefaultNotification();
        saveToken(1L, "token-fail", DevicePlatform.IOS);
        saveToken(1L, "token-ok", DevicePlatform.ANDROID);
        doThrow(new CustomException(FcmSenderErrorCode.FCM_SEND_FAILED))
                .when(fcmSender).send(eq("token-fail"), anyString(), anyString());

        //when
        notificationService.send(1L, NotificationTypeCode.CERTIFICATION, "xeulbn");

        //then
        verify(fcmSender).send(eq("token-ok"), anyString(), anyString());
        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("FCM이 UNREGISTERED 응답을 주면 해당 토큰은 DB에서 삭제된다")
    void deadTokenCleanup_unregistered() {
        // given
        saveDefaultNotification();
        fcmTokenRepository.save(FcmToken.create(1L, "dead-token", DevicePlatform.ANDROID));
        fcmTokenRepository.save(FcmToken.create(1L, "alive-token", DevicePlatform.IOS));

        // dead-token은 UNREGISTERED 에러
        doThrow(new CustomException(FcmSenderErrorCode.FCM_TOKEN_UNREGISTERED))
                .when(fcmSender).send(eq("dead-token"), anyString(), anyString());
        // alive-token은 성공

        // when
        notificationService.send(1L, NotificationTypeCode.CERTIFICATION, "xeulbn");

        // then — 죽은 토큰은 삭제
        assertThat(fcmTokenRepository.findByToken("dead-token")).isEmpty();
        // then — 살아있는 토큰은 유지
        assertThat(fcmTokenRepository.findByToken("alive-token")).isPresent();
    }

    @Test
    @DisplayName("일시적 네트워크 오류(FCM_SEND_FAILED)는 토큰을 삭제하지 않는다")
    void deadTokenCleanup_preservesOnTemporaryFailure() {
        // given
        saveDefaultNotification();
        fcmTokenRepository.save(FcmToken.create(1L, "test-token", DevicePlatform.ANDROID));

        doThrow(new CustomException(FcmSenderErrorCode.FCM_SEND_FAILED))
                .when(fcmSender).send(eq("test-token"), anyString(), anyString());

        // when
        notificationService.send(1L, NotificationTypeCode.CERTIFICATION, "xeulbn");

        // then — 일시 오류니까 보존
        assertThat(fcmTokenRepository.findByToken("test-token")).isPresent();
    }

    private Notification saveDefaultNotification() {
        return notificationRepository.save(
                Notification.create(certificationType, DEFAULT_TITLE, DEFAULT_TEMPLATE)
        );
    }

    private FcmToken saveToken(Long userId, String token, DevicePlatform platform) {
        return fcmTokenRepository.save(FcmToken.create(userId, token, platform));
    }
}