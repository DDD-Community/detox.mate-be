package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;

import java.util.HashMap;
import java.util.Map;

public record NotificationPayload(
        NotificationTargetType targetType,
        Long targetId
) {

    public NotificationPayload {
        if (targetType == null) {
            targetType = NotificationTargetType.NONE;
        }
        if (targetType != NotificationTargetType.NONE && targetId == null) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_TARGET_ID_REQUIRED);
        }
    }

    public static NotificationPayload none() {
        return new NotificationPayload(NotificationTargetType.NONE, null);
    }

    public static NotificationPayload group(Long groupId) {
        return new NotificationPayload(NotificationTargetType.GROUP, groupId);
    }

    public static NotificationPayload feed(Long groupChallengeId) {
        return new NotificationPayload(NotificationTargetType.FEED, groupChallengeId);
    }

    public static NotificationPayload feedDetail(Long challengeRecordId) {
        return new NotificationPayload(NotificationTargetType.FEED_DETAIL, challengeRecordId);
    }

    public static NotificationPayload groupChallenge(Long groupChallengeId) {
        return new NotificationPayload(NotificationTargetType.GROUP_CHALLENGE, groupChallengeId);
    }

    public Map<String, String> toFcmData(NotificationTypeCode typeCode) {
        Map<String, String> data = new HashMap<>();
        data.put("type", typeCode.name());
        data.put("targetType", targetType.name());

        if (targetId != null) {
            data.put("targetId", String.valueOf(targetId));
        }

        return data;
    }
}
