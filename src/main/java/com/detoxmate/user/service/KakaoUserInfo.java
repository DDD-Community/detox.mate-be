package com.detoxmate.user.service;

public record KakaoUserInfo(
        String providerUserId,
        String nickname,
        String email
) {
    public KakaoUserInfo(String providerUserId, String nickname) {
        this(providerUserId, nickname, null);
    }
}
