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
    public FcmToken register(Long userId, String token, DevicePlatform platform) {
        return fcmTokenRepository.findByToken(token)
                .map(existing -> {
                    existing.reassignTo(userId, platform);
                    return existing;
                })
                .orElseGet(() -> fcmTokenRepository.save(FcmToken.create(userId, token, platform)));
    }

    @Transactional
    public void remove(Long userId, String token) {
        fcmTokenRepository.deleteByUserIdAndToken(userId, token);
    }

}
