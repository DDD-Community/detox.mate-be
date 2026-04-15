package com.detoxmate.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTypeTest {

    @Test
    @DisplayName("알림 타입을 생성한다.")
    void createNotificationType(){
        //given
        NotificationTypeCode typeCode = NotificationTypeCode.CERTIFICATION;

        //when
        NotificationType notificationType = NotificationType.create(typeCode);

        //then
        assertThat(notificationType.getTypeCode()).isEqualTo(NotificationTypeCode.CERTIFICATION);
    }

    @Test
    @DisplayName("알림 타입 코드는 null일 수 없다.")
    void notificationTypeCodeNotNull(){
        //when&then
        assertThatThrownBy(()->NotificationType.create(null))
                .isInstanceOf(NotificationException.class)
                .extracting(e->((NotificationException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.INVALIDE_TYPE_CODE);
    }

    @Test
    @DisplayName("알림 타입과 일림은 연관관계이다.")
    void notificationTypeRelatedToNotification(){
        //given
        NotificationTypeCode typeCode = NotificationTypeCode.COMMENT;
        Notification notification = Notification.create(
                "새로운 댓글!",
                "{nickname}님이 댓글을 남겼습니다.",
                typeCode
        );

        //when
        NotificationType notificationType = NotificationType.create(typeCode);

        //then
        assertThat(notificationType.getTypeCode()).isEqualTo(notification.getTypeCode());
    }

}