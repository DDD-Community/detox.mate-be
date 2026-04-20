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
    @DisplayName("알림 템플릿과 수신자, 행위자 닉네임으로 알림 이력을 생성합니다.")
    void createNotificationHistory() {
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        Long recipientId = 1L;
        String actorNickname= "xeulbn";

        //when
        NotificationHistory history = NotificationHistory.from(notification,recipientId,actorNickname);

        //then
        assertThat(history.getUserId()).isEqualTo(recipientId);
        assertThat(history.getMessage()).isEqualTo("xeulbn님이 댓글을 남겼습니다.");
        assertThat(history.getNotification()).isEqualTo(notification);
    }

    @Test
    @DisplayName("닉네임 플레이스홀더가 없는 템플릿으로도 알림 이력을 생성할 수 있다.")
    void createNotificationHistoryWithoutPlaceHolder(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 시간 알림",
                "오늘 인증까지 1시간 남았습니다."
        );
        Long recipientId = 1L;

        //when
        NotificationHistory history = NotificationHistory.from(notification,recipientId,null);

        //then
        assertThat(history.getUserId()).isEqualTo(recipientId);
        assertThat(history.getMessage()).isEqualTo("오늘 인증까지 1시간 남았습니다.");
    }

    @Test
    @DisplayName("알림 이력의 수신자 ID는 null일 수 없다.")
    void userIdNotNull(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );

        //when & then
        assertThatThrownBy(()-> NotificationHistory.from(notification,null,"xeulbn"))
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
        assertThatThrownBy(()-> Notification.from(null,recipientId,"xeulbn"))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_HISTORY_NOTIFICATION_REQUIRED);
    }

    @Test
    @DisplayName("닉네임이 필요한 템플릿인데, 닉네임이 null이면 예외가 발생한다.")
    void createHistoryWithNullNicknameWhenPlaceholderExists(){
        // given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        Long recipientId = 1L;

        //when & then
        assertThatThrownBy(()-> NotificationHistory.from(notification,recipientId,null))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NICKNAME_REQUIRED);
    }
    
    @Test
    @DisplayName("알림 이력 생성 시 원본 템플릿의 제목을 스냅샷으로 저장한다.")
    void historyTitleSnapshotFromTemplate(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );

        //when
        NotificationHistory history = NotificationHistory.from(notification,1L,"xeulbn");

        //then
        assertThat(history.getTitle()).isEqualTo("댓글 알림");
    }

    @Test
    @DisplayName("알림 이력 생성 시 is_read는 false로 초기화된다.")
    void historyDefaultIsReadFalse(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );

        //when
        NotificationHistory history = NotificationHistory.from(notification,1L,"xeulbn");

        //then
        assertThat(history.isRead()).isFalse();
    }

    @Test
    @DisplayName("알림 이력 생성 시 createdAt은 현재 시점으로 설정된다.")
    void historyCreatedAtOnCreation(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        LocalDateTime before = LocalDateTime.now();

        //when
        NotificationHistory history = NotificationHistory.from(notification,1L,"xeulbn");

        //then
        assertThat(history.getCreatedAt()).isBetween(before,LocalDateTime.now());
    }

    @Test
    @DisplayName("알림을 읽으면 is_read가 true가 된다.")
    void markAsRead(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        NotificationHistory history = NotificationHistory.from(notification,1L,"xeulbn");

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
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 시간 알림",
                "오늘 인증까지 1시간 남았습니다."
        );
        LocalDateTime pastExpiration = LocalDateTime.now().minusHours(1);
        NotificationHistory history = NotificationHistory.from(notification,1L,null,pastExpiration);

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
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 시간 알림",
                "오늘 인증까지 1시간 남았습니다."
        );
        LocalDateTime futureExpriation = LocalDateTime.now().plusHours(1);
        NotificationHistory history = NotificationHistory.from(notification,1L,null,futureExpriation);

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
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        NotificationHistory history = NotificationHistory.from(notification,1L,"xeulbn");

        //when
        boolean expired = history.isExpired();

        //then
        assertThat(expired).isFalse();
    }

}