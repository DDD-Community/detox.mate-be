package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    @DisplayName("notification 템플릿을 정상적으로 생성한다.")
    void createNotificiation(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.REACTION);
        String title = "반응 알림";
        String messageTemplate = "{nickname}님이 반응을 남겼습니다.";

        //when
        Notification notification = Notification.of(type,title,messageTemplate);

        //then
        assertThat(notification.getType()).isEqualTo(type);
        assertThat(notification.getTitle()).isEqualTo(title);
        assertThat(notification.getMessageTemplate()).isEqualTo(messageTemplate);

    }

    @Test
    @DisplayName("notification 제목은 50자를 초과할 수 없다.")
    void notificationTitleMaxLength(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.REACTION);
        String overLengthTitle = "a".repeat(51);
        String messageTemplate = "{nickname}님이 반응을 남겼습니다.";

        //when & then
        assertThatThrownBy(() -> Notification.of(type, overLengthTitle, messageTemplate))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.TITLE_TOO_LONG);

    }

    @Test
    @DisplayName("알림 메시지는 255자를 초과할 수 없다.")
    void notificationTitleLength(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.REACTION);
        String title = "반응 알림";
        String overLengthMessage = "a".repeat(256);

        //when & then
        assertThatThrownBy(()-> Notification.of(type,title,overLengthMessage))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.MESSAGE_TOO_LONG);
    }

    @Test
    @DisplayName("알림 템플릿의 메시지에 닉네임을 치환한다.")
    void resolveTemplate(){
        //given
        Notification notification = Notification.of(
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 알림",
                "{nickname}님이 인증을 완료했습니다."
        );
        String nickname = "xeulbn";

        //when
        String resolved = notification.resolve(nickname);

        //then
        assertThat(resolved).isEqualTo("xeulbn님이 인증을 완료했습니다.");

    }

    @Test
    @DisplayName("템플릿에 닉네임 플레이스홀더가 있는데 닉네임이 null이면 예외가 발생한다.")
    void resolveWithNullNickName(){
        //given
        Notification notification = Notification.of(
                NotificationType.create(NotificationTypeCode.COMMENT),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );

        //when & then
        assertThatThrownBy(()-> notification.resolve(null))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NICKNAME_REQUIRED);
    }
    
    @Test
    @DisplayName("템플릿에 닉네임 플레이스홀더가 없으면 닉네임이 null이어도 원본 메시지를 그대로 반환한다.")
    void resolveWithoutPlaceholder(){
        //given
        Notification notification = Notification.of(
                NotificationType.create(NotificationTypeCode.CERTIFICATION),
                "인증 시간 알림",
                "오늘 인증까지 1시간 남았습니다."
        );

        //when
        String resolved = notification.resolve(null);

        //then
        assertThat(resolved).isEqualTo("오늘 인증까지 1시간 남았습니다.");
    }

}