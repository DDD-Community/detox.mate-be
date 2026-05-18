package com.detoxmate.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePushNotificationSettingRequest(
        @NotNull
        Boolean pushNotificationEnabled
) {}
