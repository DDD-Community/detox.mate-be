package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public FcmToken register (Long userId, String token, DevicePlatform devicePlatform) {
        fcmTokenRepository.deleteByToken(token);
        return fcmTokenRepository.save(FcmToken.create(userId, token, devicePlatform));
    }

    @Transactional
    public void remove(String token) {
        fcmTokenRepository.deleteByToken(token);
    }

}
