package com.detoxmate.applock.dto;

import com.detoxmate.applock.domain.App;

public record AppResponse(
        Long appId,
        Long userId,
        String appDisplayName,
        Integer dailyLimitMinutes
) {

    public static AppResponse from(App app) {
        return new AppResponse(
                app.getId(),
                app.getUser().getId(),
                app.getAppDisplayName(),
                app.getAppTimeLimit().getDailyLimitMinutes()
        );
    }
}
