package com.detoxmate.user.dto;

public record MyPageResponse(
        String displayName,
        String userCode,
        String profileImageUrl
) {
    public static MyPageResponse from(MyProfileResponse profile) {
        return new MyPageResponse(profile.displayName(), profile.userCode(), profile.profileImageUrl());
    }
}
