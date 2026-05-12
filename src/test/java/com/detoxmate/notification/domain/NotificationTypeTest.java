package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTypeTest {

    @Test
    @DisplayName("알림 타입을 생성한다.")
    void createNotificationType(){
        //given
        NotificationTypeCode typeCode = NotificationTypeCode.CERTIFICATION_CREATED;

        //when
        NotificationType notificationType = NotificationType.create(typeCode);

        //then
        assertThat(notificationType.getTypeCode()).isEqualTo(NotificationTypeCode.CERTIFICATION_CREATED);
    }

    @Test
    @DisplayName("알림 타입 코드는 null일 수 없다.")
    void notificationTypeCodeNotNull(){
        //when&then
        assertThatThrownBy(()->NotificationType.create(null))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.INVALID_TYPE_CODE);
    }

}