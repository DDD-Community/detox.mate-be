package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationHistoryTest {

    @Test
    @DisplayName("알림 템플릿과 수신자, 치환된 메시지로 알림 이력을 생성합니다.")
    void createNotificationHistory() {
        //given
        String notificationTitle = "댓글 알림";
        String notificationMessageTemplate = "{nickname}님이 댓글을 남겼습니다.";

        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                notificationTitle,
                notificationMessageTemplate
        );
        Long recipientId = 1L;
        String resolveNotificationMessage = "xeulbn님이 댓글을 남겼습니다.";

        //when
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                recipientId,
                resolveNotificationMessage
        );

        //then
        assertThat(history.getUserId()).isEqualTo(recipientId);
        assertThat(history.getMessage()).isEqualTo(resolveNotificationMessage);
        assertThat(history.getNotification()).isEqualTo(notification);
    }

    @Test
    @DisplayName("닉네임 플레이스홀더가 없는 템플릿으로도 알림 이력을 생성할 수 있다.")
    void createNotificationHistoryWithoutPlaceHolder(){
        //given
        String notificationTitle = "인증 시간 알림";
        String notificationMessageTemplate = "오늘 인증까지 1시간 남았습니다.";

        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION_CREATED),
                notificationTitle,
                notificationMessageTemplate
        );
        Long recipientId = 1L;

        //when
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                recipientId,
                notificationMessageTemplate
        );

        //then
        assertThat(history.getUserId()).isEqualTo(recipientId);
        assertThat(history.getMessage()).isEqualTo(notificationMessageTemplate);
    }

    @Test
    @DisplayName("알림 이력의 수신자 ID는 null일 수 없다.")
    void userIdNotNull(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );

        //when & then
        assertThatThrownBy(()-> NotificationHistory.fromResolvedMessage(notification,null,"xeulbn님이 댓글을 남겼습니다."))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_HISTORY_USER_ID_REQUIRED);
    }

    @Test
    @DisplayName("알림 이력의 원본 템플릿은 null일 수 없다.")
    void notificationNotNull(){
        //given
        Long recipientId = 1L;

        //when & then
        assertThatThrownBy(()-> NotificationHistory.fromResolvedMessage(null,recipientId,"xeulbn님이 댓글을 남겼습니다."))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_HISTORY_NOTIFICATION_REQUIRED);
    }

    @Test
    @DisplayName("알림 이력의 메시지는 null일 수 없다.")
    void createHistoryWithNullMessage(){
        // given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        Long recipientId = 1L;

        //when & then
        assertThatThrownBy(()-> NotificationHistory.fromResolvedMessage(notification,recipientId,null))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_HISTORY_MESSAGE_REQUIRED);
    }
    
    @Test
    @DisplayName("알림 이력 생성 시 원본 템플릿의 제목을 스냅샷으로 저장한다.")
    void historyTitleSnapshotFromTemplate(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );

        //when
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                1L,
                "xeulbn님이 댓글을 남겼습니다"
        );

        //then
        assertThat(history.getTitle()).isEqualTo("댓글 알림");
    }

    @Test
    @DisplayName("알림 이력 생성 시 is_read는 false로 초기화된다.")
    void historyDefaultIsReadFalse(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );

        //when
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                1L,
                "xeulbn님이 댓글을 남겼습니다."
        );

        //then
        assertThat(history.isRead()).isFalse();
    }

    @Test
    @DisplayName("알림 이력 생성 시 발신자 ID를 저장할 수 있다.")
    void historyStoresSenderUserId(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );

        //when
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                1L,
                2L,
                "xeulbn님이 댓글을 남겼습니다.",
                NotificationPayload.none(),
                null
        );

        //then
        assertThat(history.getSenderUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("알림 이력 생성 시 createdAt은 현재 시점으로 설정된다.")
    void historyCreatedAtOnCreation(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        LocalDateTime before = LocalDateTime.now();

        //when
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                1L,
                "xeulbn님이 댓글을 남겼습니다."
        );

        //then
        assertThat(history.getCreatedAt()).isBetween(before,LocalDateTime.now());
    }

    @Test
    @DisplayName("알림을 읽으면 is_read가 true가 된다.")
    void markAsRead(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                1L,
                "xeulbn님이 댓글을 남겼습니다."
        );

        //when
        history.markAsRead();

        //then
        assertThat(history.isRead()).isTrue();
    }

    @Test
    @DisplayName("만료 시점이 지났으면 만료된 것으로 판단한다.")
    void isExpiredAfterExpireDate(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION_CREATED),
                "인증 시간 알림",
                "오늘 인증까지 1시간 남았습니다."
        );
        LocalDateTime pastExpiration = LocalDateTime.now().minusHours(1);
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                1L,
                "오늘 인증까지 1시간 남았습니다.",
                pastExpiration
        );

        //when
        boolean expired = history.isExpired();

        //then
        assertThat(expired).isTrue();
    }

    @Test
    @DisplayName("만료 시점이 미래면 만료되지 않은 것으로 판단한다.")
    void isNotExpiredAfterExpireDate(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION_CREATED),
                "인증 시간 알림",
                "오늘 인증까지 1시간 남았습니다."
        );
        LocalDateTime futureExpriation = LocalDateTime.now().plusHours(1);
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                1L,
                "오늘 인증까지 1시간 남았습니다.",
                futureExpriation
        );

        //when
        boolean expired = history.isExpired();

        //then
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("만료 시점이 null이면 만료되지 않은 알림으로 판단한다.")
    void isNotExpiredWhenExpiredAtIsNull(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                1L,
                "xeulbn님이 댓글을 남겼습니다."
        );

        //when
        boolean expired = history.isExpired();

        //then
        assertThat(expired).isFalse();
    }

}
