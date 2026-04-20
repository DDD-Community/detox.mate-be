package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

}