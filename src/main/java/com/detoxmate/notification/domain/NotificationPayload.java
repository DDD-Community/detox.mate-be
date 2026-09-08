package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;

import java.util.HashMap;
import java.util.Map;

public record NotificationPayload(
        NotificationTargetType targetType,
        Long targetId,
        NotificationSourceType sourceType,
        Long sourceId
) {

    public NotificationPayload {
        if (targetType == null) {
            targetType = NotificationTargetType.NONE;
        }
        if (sourceType == null) {
            sourceType = NotificationSourceType.NONE;
        }
        if (requiresTargetId(targetType) && targetId == null) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_TARGET_ID_REQUIRED);
        }
        if (sourceType != NotificationSourceType.NONE && sourceId == null) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_SOURCE_ID_REQUIRED);
        }
    }

    public static NotificationPayload none() {
        return new NotificationPayload(NotificationTargetType.NONE, null, NotificationSourceType.NONE, null);
    }

    public static NotificationPayload group(Long groupId) {
        return new NotificationPayload(NotificationTargetType.GROUP, groupId, NotificationSourceType.NONE, null);
    }

    public static NotificationPayload feed(Long groupChallengeId) {
        return new NotificationPayload(NotificationTargetType.FEED, groupChallengeId, NotificationSourceType.NONE, null);
    }

    public static NotificationPayload feedDetail(Long challengeRecordId) {
        return new NotificationPayload(NotificationTargetType.FEED_DETAIL, challengeRecordId, NotificationSourceType.CHALLENGE_RECORD, challengeRecordId);
    }

    public static NotificationPayload commentFeedDetail(Long challengeRecordId, Long commentId) {
        return new NotificationPayload(NotificationTargetType.FEED_DETAIL, challengeRecordId, NotificationSourceType.COMMENT, commentId);
    }

    public static NotificationPayload myPage() {
        return new NotificationPayload(NotificationTargetType.MY_PAGE, null, NotificationSourceType.NONE, null);
    }

    public static NotificationPayload appUnlockDurationSetting() {
        return new NotificationPayload(
                NotificationTargetType.APP_UNLOCK_DURATION_SETTING,
                null,
                NotificationSourceType.NONE,
                null
        );
    }

    private static boolean requiresTargetId(NotificationTargetType targetType) {
        return targetType != NotificationTargetType.NONE
                && targetType != NotificationTargetType.MY_PAGE
                && targetType != NotificationTargetType.APP_UNLOCK_DURATION_SETTING;
    }


    public Map<String, String> toFcmData(NotificationTypeCode typeCode) {
        Map<String, String> data = new HashMap<>();
        data.put("type", typeCode.name());
        data.put("targetType", targetType.name());

        if (targetId != null) {
            data.put("targetId", String.valueOf(targetId));
        }

        if (sourceType != NotificationSourceType.NONE) {
            data.put("sourceType", sourceType.name());
        }

        if (sourceId != null) {
            data.put("sourceId", String.valueOf(sourceId));
        }

        return data;
    }
}
