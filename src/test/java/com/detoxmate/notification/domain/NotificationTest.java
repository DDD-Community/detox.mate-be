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
        NotificationType type = NotificationType.create(NotificationTypeCode.REACTION_CREATED);
        String title = "반응 알림";
        String messageTemplate = "{nickname}님이 반응을 남겼습니다.";

        //when
        Notification notification = Notification.create(type,title,messageTemplate);

        //then
        assertThat(notification.getType()).isEqualTo(type);
        assertThat(notification.getTitle()).isEqualTo(title);
        assertThat(notification.getMessageTemplate()).isEqualTo(messageTemplate);

    }

    @Test
    @DisplayName("notification 제목은 50자를 초과할 수 없다.")
    void notificationTitleMaxLength(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.REACTION_CREATED);
        String overLengthTitle = "a".repeat(51);
        String messageTemplate = "{nickname}님이 반응을 남겼습니다.";

        //when & then
        assertThatThrownBy(() -> Notification.create(type, overLengthTitle, messageTemplate))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_TITLE_LENGTH_EXCEEDED);

    }

    @Test
    @DisplayName("타입 코드가 null이면 생성할 수 없다.")
    void createNotificationTypeWithNullCode(){
        //given
        String title = "반응 알림";
        String messageTemplate =  "{nickname}님이 반응을 남겼습니다.";

        //when & then
        assertThatThrownBy(() -> Notification.create(null,title,messageTemplate))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_TYPE_REQUIRED);
    }

    @Test
    @DisplayName("알림 타이틀이 null이면 생성할 수 없다.")
    void createNotificationTitleWithNullCode(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.REACTION_CREATED);
        String messageTemplate = "{nickname}님이 반응을 남겼습니다.";

        //when & then
        assertThatThrownBy(() -> Notification.create(type,null,messageTemplate))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_TITLE_REQUIRED);
    }

    @Test
    @DisplayName("알림 메시지템플릿이 null이면 생성할 수 없다.")
    void createNotificationMessageTemplateWithNullCode(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.REACTION_CREATED);
        String title = "반응 알림";

        //when & then
        assertThatThrownBy(() -> Notification.create(type,title,null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_MESSAGE_TEMPLATE_REQUIRED);
    }

    @Test
    @DisplayName("알림 메시지는 255자를 초과할 수 없다.")
    void notificationTitleLength(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.REACTION_CREATED);
        String title = "반응 알림";
        String overLengthMessage = "a".repeat(256);

        //when & then
        assertThatThrownBy(()-> Notification.create(type,title,overLengthMessage))
                .isInstanceOf(CustomException.class)
                .extracting(e-> ((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_MESSAGE_TEMPLATE_LENGTH_EXCEEDED);
    }

    @Test
    @DisplayName("알림 템플릿의 메시지에 컨텍스트 변수를 치환한다.")
    void resolveTemplate(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION_CREATED),
                "인증 알림",
                "{nickname}님이 {groupName}에서 인증을 완료했습니다."
        );

        //when
        String resolved = notification.resolve(NotificationContext.of(
                "nickname", "xeulbn",
                "groupName", "디톡스메이트"
        ));

        //then
        assertThat(resolved).isEqualTo("xeulbn님이 디톡스메이트에서 인증을 완료했습니다.");

    }

    @Test
    @DisplayName("템플릿에 필요한 컨텍스트 변수가 없으면 예외가 발생한다.")
    void resolveWithMissingVariable(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );

        //when & then
        assertThatThrownBy(()-> notification.resolve(NotificationContext.empty()))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException)e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_CONTEXT_MISSING_VARIABLE);
    }
    
    @Test
    @DisplayName("템플릿에 플레이스홀더가 없으면 빈 컨텍스트로도 원본 메시지를 그대로 반환한다.")
    void resolveWithoutPlaceholder(){
        //given
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.CERTIFICATION_CREATED),
                "인증 시간 알림",
                "오늘 인증까지 1시간 남았습니다."
        );

        //when
        String resolved = notification.resolve(NotificationContext.empty());

        //then
        assertThat(resolved).isEqualTo("오늘 인증까지 1시간 남았습니다.");
    }

}
