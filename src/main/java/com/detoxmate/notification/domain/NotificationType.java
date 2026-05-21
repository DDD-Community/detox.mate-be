package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationType {

    @Id
    @Column(name = "notification_type_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type_name",nullable = false, length = 50)
    private NotificationTypeCode typeCode;

    private NotificationType(NotificationTypeCode typeCode) {
        this.typeCode = typeCode;
    }

    public static NotificationType create(NotificationTypeCode typeCode) {
        if(typeCode == null) {
            throw new CustomException(NotificationErrorCode.INVALID_TYPE_CODE);
        }
        return new NotificationType(typeCode);
    }
}
