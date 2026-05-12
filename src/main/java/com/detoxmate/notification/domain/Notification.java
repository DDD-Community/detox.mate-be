package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.regex.Pattern;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    private static final int TITLE_MAX_LENGTH = 50;
    private static final int MESSAGE_MAX_LENGTH = 255;
    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile(".*\\{[^}]+}.*");


    @Id
    @Column(name = "notification_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_title",nullable = false,length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "notification_message_template",nullable = false,length = MESSAGE_MAX_LENGTH)
    private String messageTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_type_id",nullable = false)
    private NotificationType type;

    private Notification(NotificationType type, String title, String messageTemplate){
        this.type = type;
        this.title = title;
        this.messageTemplate = messageTemplate;
    }

    public static Notification create(NotificationType type, String title, String messageTemplate){
        validateType(type);
        validateTitle(title);
        validateMessageTemplate(messageTemplate);
        return new Notification(type, title, messageTemplate);
    }


    public String resolve(NotificationContext context) {
        NotificationContext safeContext = context == null ? NotificationContext.empty() : context;

        String resolved = messageTemplate;

        for (Map.Entry<String, String> entry : safeContext.variables().entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        if (UNRESOLVED_PLACEHOLDER.matcher(resolved).matches()) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_CONTEXT_MISSING_VARIABLE);
        }

        return resolved;
    }

    private static void validateType(NotificationType type){
        if(type == null){
            throw new CustomException(NotificationErrorCode.NOTIFICATION_TYPE_REQUIRED);
        }
    }

    private static void validateTitle(String title){
        if(title==null || title.isBlank()){
            throw new CustomException(NotificationErrorCode.NOTIFICATION_TITLE_REQUIRED);
        }
        if(title.length()>TITLE_MAX_LENGTH){
            throw new CustomException(NotificationErrorCode.NOTIFICATION_TITLE_LENGTH_EXCEEDED);
        }
    }

    private static void validateMessageTemplate(String messageTemplate){
        if(messageTemplate==null || messageTemplate.isBlank()){
            throw new CustomException(NotificationErrorCode.NOTIFICATION_MESSAGE_TEMPLATE_REQUIRED);
        }

        if(messageTemplate.length()>MESSAGE_MAX_LENGTH){
            throw new CustomException(NotificationErrorCode.NOTIFICATION_MESSAGE_TEMPLATE_LENGTH_EXCEEDED);
        }
    }

}
