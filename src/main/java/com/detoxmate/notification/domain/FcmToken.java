package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.FcmTokenErrorCode;
import com.google.firebase.database.core.Platform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fcm_token_token", columnNames = "token")
        },
        indexes = {
                @Index(name = "idx_fcm_token_user_id",columnList = "user_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken {

    private static final int TOKEN_MAX_LENGTH = 4096;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, length = TOKEN_MAX_LENGTH)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private DevicePlatform platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private FcmToken(Long userId, String token, DevicePlatform platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.createdAt = LocalDateTime.now();
    }

    public static FcmToken create(Long userId, String token, DevicePlatform platform) {
        validate(userId, token, platform);
        return new FcmToken(userId,token,platform);
    }

    private static void validate(Long userId, String token, DevicePlatform platform) {
        if(userId==null){
            throw new CustomException(FcmTokenErrorCode.USER_ID_REQUIRED);
        }

        if(token ==null || token.isBlank()){
            throw new CustomException(FcmTokenErrorCode.TOKEN_REQUIRED);
        }

        if(token.length() > TOKEN_MAX_LENGTH){
            throw new CustomException(FcmTokenErrorCode.TOKEN_TOO_LONG);
        }

        if(platform==null){
            throw new CustomException(FcmTokenErrorCode.PLATFORM_REQUIRED);
        }
    }
}
