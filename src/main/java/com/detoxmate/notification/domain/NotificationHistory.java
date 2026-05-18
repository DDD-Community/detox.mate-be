package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistory {

    private static final int TITLE_MAX_LENGTH = 50;
    private static final int MESSAGE_MAX_LENGTH = 255;

    @Id
    @Column(name = "notification_history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id",nullable = false)
    private Notification notification;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "notification_history_title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "notification_history_message", nullable = false, length = MESSAGE_MAX_LENGTH)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type",nullable = false,length = 30)
    private NotificationTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private NotificationSourceType sourceType;

    @Column(name = "source_id")
    private Long sourceId;


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    private NotificationHistory(Notification notification, Long userId, String title, String message,
                                NotificationTargetType targetType, Long targetId,
                                NotificationSourceType sourceType, Long sourceId,
                                LocalDateTime createdAt, LocalDateTime expiredAt){
        this.notification = notification;
        this.userId = userId;
        this.isRead = false;
        this.title = title;
        this.message = message;
        this.targetType = targetType;
        this.targetId = targetId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
    }

    public static NotificationHistory fromResolvedMessage(Notification notification, Long userId, String message) {
        return fromResolvedMessage(notification, userId, message, NotificationPayload.none(), null);
    }

    public static NotificationHistory fromResolvedMessage(Notification notification,
                                                          Long userId,
                                                          String message,
                                                          LocalDateTime expiredAt) {

        return fromResolvedMessage(notification, userId, message, NotificationPayload.none(), expiredAt);
    }

    public static NotificationHistory fromResolvedMessage(Notification notification, Long userId, String message, NotificationPayload payload) {
        return fromResolvedMessage(notification, userId, message, payload, null);
    }

    public static NotificationHistory fromResolvedMessage(Notification notification,
                                                          Long userId,
                                                          String message,
                                                          NotificationPayload payload,
                                                          LocalDateTime expiredAt) {
        validateNotification(notification);
        validateUserId(userId);
        validateMessage(message);

        NotificationPayload safePayload = payload == null ? NotificationPayload.none() : payload;

        return new NotificationHistory(
                notification,
                userId,
                notification.getTitle(),
                message,
                safePayload.targetType(),
                safePayload.targetId(),
                safePayload.sourceType(),
                safePayload.sourceId(),
                LocalDateTime.now(),
                expiredAt
        );
    }


    public void markAsRead(){
        this.isRead = true;
    }

    public boolean isExpired(){
        if(expiredAt ==null){
            return false;
        }
        return LocalDateTime.now().isAfter(expiredAt);
    }

    private static void validateNotification(Notification notification){
        if(notification == null){
            throw new CustomException(NotificationErrorCode.NOTIFICATION_HISTORY_NOTIFICATION_REQUIRED);
        }
    }

    private static void validateUserId(Long userId){
        if(userId ==null){
            throw new CustomException(NotificationErrorCode.NOTIFICATION_HISTORY_USER_ID_REQUIRED);
        }
    }

    private static void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_HISTORY_MESSAGE_REQUIRED);
        }

        if (message.length() > MESSAGE_MAX_LENGTH) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_HISTORY_MESSAGE_LENGTH_EXCEEDED);
        }
    }

}
