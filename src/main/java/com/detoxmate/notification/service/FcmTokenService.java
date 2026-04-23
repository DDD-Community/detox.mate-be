package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.notification.util.TokenMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public FcmToken register (Long userId, String token, DevicePlatform devicePlatform) {
        fcmTokenRepository.deleteByToken(token);
        log.info("[Notification][fcm-token-register] userId={}, platform={}, token={}",
                userId, devicePlatform, TokenMasker.mask(token));
        return fcmTokenRepository.save(FcmToken.create(userId, token, devicePlatform));
    }

    @Transactional
    public void remove(String token) {
        fcmTokenRepository.deleteByToken(token);
    }

}
