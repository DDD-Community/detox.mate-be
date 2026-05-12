package com.detoxmate.notification.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import com.detoxmate.notification.domain.NotificationContext;
import com.detoxmate.notification.domain.NotificationPayload;
import com.detoxmate.notification.domain.NotificationTypeCode;

public record NotificationCommand(Long recipientUserId,
                                  NotificationTypeCode typeCode,
                                  NotificationContext context,
                                  NotificationPayload payload,
                                  boolean saveHistory) {

    public NotificationCommand {
        if (recipientUserId == null) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_RECIPIENT_REQUIRED);
        }

        if (typeCode == null) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_COMMAND_TYPE_REQUIRED);
        }

        if (context == null) {
            context = NotificationContext.empty();
        }
        if (payload == null) {
            payload = NotificationPayload.none();
        }
    }

    public static NotificationCommand history(Long recipientUserId,
                                              NotificationTypeCode typeCode,
                                              NotificationContext context,
                                              NotificationPayload payload) {
        return new NotificationCommand(recipientUserId, typeCode, context, payload, true);
    }

    public static NotificationCommand pushOnly(Long recipientUserId,
                                               NotificationTypeCode typeCode,
                                               NotificationContext context,
                                               NotificationPayload payload) {
        return new NotificationCommand(recipientUserId, typeCode, context, payload, false);
    }
}
