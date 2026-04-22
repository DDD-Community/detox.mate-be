package com.detoxmate.notification.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.FcmSenderErrorCode;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import com.detoxmate.notification.domain.*;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.notification.repository.NotificationRepository;
import com.detoxmate.notification.util.FcmSender;
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

    @Autowired
    NotificationService notificationService;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    FcmTokenRepository fcmTokenRepository;
    @Autowired
    NotificationHistoryRepository historyRepository;

    @MockitoBean
    FcmSender fcmSender;

    @Test
    @DisplayName("사용자에게 알림을 전송하면 FCM 푸시가 발송되고 이력이 저장된다")
    void sendNotification_toSingleDevice(){
        //given
        notificationRepository.save(Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 시간 알림",
                "{nickname}님, 오늘 인증까지 1시간 남았습니다."
        ));
        fcmTokenRepository.save(FcmToken.create(1L,"test-token-abc", DevicePlatform.IOS));

        //when
        notificationService.send(1L, NotificationTypeCode.CERTIFICATION,"xeulbn");

        //then
        List<NotificationHistory> histories = historyRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getUserId()).isEqualTo(1L);

        //then
        verify(fcmSender).send(
                eq("test-token-abc"),
                eq("인증 시간 알림"),
                eq("xeulbn님, 오늘 인증까지 1시간 남았습니다.")
        );
    }

    @Test
    @DisplayName("사용자의 여러 디바이스에 각각 FCM 푸시가 전송된다")
    void sendNOtification_toMultipleDevices(){
        //given
        notificationRepository.save(Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 시간 알림",
                "인증까지 1시간 남았습니다."
        ));
        fcmTokenRepository.save(FcmToken.create(1L,"test-token-abc-IOS", DevicePlatform.IOS));
        fcmTokenRepository.save(FcmToken.create(1L,"test-token-abc-AND", DevicePlatform.ANDROID));

        //when
        notificationService.send(1L, NotificationTypeCode.CERTIFICATION, "xeulbn");

        //then - 각 디바이스로의 전송
        verify(fcmSender).send(eq("test-token-abc-IOS"),anyString(),anyString());
        verify(fcmSender).send(eq("test-token-abc-AND"),anyString(),anyString());
        verify(fcmSender,times(2)).send(anyString(),anyString(),anyString());

        //then - 이력은 한 번만 저장 (디바이스 수와 무관)
        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("토큰이 없는 사용자에게 전송하면 FCM 호출은 없지만 이력은 저장된다")
    void sendNotification_noTokens(){
        //given
        notificationRepository.save(Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 시간 알림",
                "인증까지 1시간 남았습니다."
        ));

        //when
        notificationService.send(999L, NotificationTypeCode.CERTIFICATION,"xeulbn");

        //then - FCM은 호출되지 않음
        verify(fcmSender,never()).send(anyString(),anyString(),anyString());

        //then - 이력은 여전히 저장됨
        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 타입의 알림을 전송하면 예외가 발생한다")
    void sendNotification_templateNotFound(){
        //given
        fcmTokenRepository.save(FcmToken.create(1L,"test-token-abc", DevicePlatform.IOS));

        //when & then
        assertThatThrownBy(()-> notificationService.send(1L,NotificationTypeCode.CERTIFICATION, "xeulbn"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);

        //then - fcm 호출 없음, 이력 저장 없음
        verify(fcmSender,never()).send(anyString(),anyString(),anyString());
        assertThat(historyRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("일부 FCM 전송이 실패해도 나머지 디바이스로는 계속 전송된다")
    void sendNotification_partialFailure_continues(){
        //given
        notificationRepository.save(Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 시간 알림",
                "인증까지 1시간 남았습니다."
        ));
        fcmTokenRepository.save(FcmToken.create(1L,"token-fail", DevicePlatform.IOS));
        fcmTokenRepository.save(FcmToken.create(1L,"token-ok", DevicePlatform.ANDROID));

        doThrow(new CustomException(
                FcmSenderErrorCode.FCM_SEND_FAILED
        )).when(fcmSender).send(eq("token-fail"),anyString(),anyString());

        //when - 예외 던지지 않음
        notificationService.send(1L,NotificationTypeCode.CERTIFICATION,"xeulbn");

        //then
        verify(fcmSender).send(eq("token-ok"),anyString(),anyString());
        assertThat(historyRepository.findAll()).hasSize(1);
    }
}