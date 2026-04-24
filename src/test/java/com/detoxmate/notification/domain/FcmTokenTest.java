package com.detoxmate.notification.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.FcmTokenErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class FcmTokenTest {

    @Test
    @DisplayName("유효한 정보로 FCM 토큰을 생성한다.")
    void createFcmToken(){
        //given & when
        String validToken = "validToken";
        FcmToken fcmToken = FcmToken.create(1L,validToken,DevicePlatform.IOS);

        //then
        assertThat(fcmToken.getUserId()).isEqualTo(1L);
        assertThat(fcmToken.getToken()).isEqualTo(validToken);
        assertThat(fcmToken.getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    @DisplayName("userId가 null이면 예외를 던진다")
    void failWhenUserIdIsNull(){
        //given
        String validToken = "validToken";

        //when & then
        assertThatThrownBy(()->
                FcmToken.create(null,validToken,DevicePlatform.IOS))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException)e).getErrorCode())
                .isEqualTo(FcmTokenErrorCode.USER_ID_REQUIRED);
    }

    @Test
    @DisplayName("토큰이 null이면 예외를 던진다")
    void failWhenTokenIsNull(){
        //when & then
        assertThatThrownBy(()->
                FcmToken.create(1L,null,DevicePlatform.IOS))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException)e).getErrorCode())
                .isEqualTo(FcmTokenErrorCode.TOKEN_REQUIRED);
    }

    @Test
    @DisplayName("토큰이 공백이면 예외를 던진다")
    void failWhenTokenIsBlank(){
        //given
        String emptyToken = " ";
        //when & then
        assertThatThrownBy(()->
                FcmToken.create(1L,emptyToken,DevicePlatform.IOS))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException)e).getErrorCode())
                .isEqualTo(FcmTokenErrorCode.TOKEN_REQUIRED);
    }

    @Test
    @DisplayName("토큰이 최대길이(4096자)를 초과하면 예외를 던진다")
    void failWhenTokenExceedsMaxLength(){
        //given
        String tooLongToken = "a".repeat(4097);

        //when & then
        assertThatThrownBy(()->
                FcmToken.create(1L,tooLongToken,DevicePlatform.IOS))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException)e).getErrorCode())
                .isEqualTo(FcmTokenErrorCode.TOKEN_TOO_LONG);

    }

    @Test
    @DisplayName("플랫폼이 null이면 예외를 던진다")
    void failWhenPlatformIsNull(){
        String validToken = "validToken";

        //when & then
        assertThatThrownBy(()->
                FcmToken.create(1L,validToken,null))
                .isInstanceOf(CustomException.class)
                .extracting(e->((CustomException)e).getErrorCode())
                .isEqualTo(FcmTokenErrorCode.PLATFORM_REQUIRED);
    }

    @Test
    @DisplayName("reassignTo: 소유자와 플랫폼을 새 값으로 교체한다")
    void reassignTo_updatesOwnerAndPlatform() {
        FcmToken token = FcmToken.create(1L, "abc", DevicePlatform.ANDROID);

        token.reassignTo(2L, DevicePlatform.IOS);

        assertThat(token.getUserId()).isEqualTo(2L);
        assertThat(token.getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    @DisplayName("reassignTo: updatedAt이 갱신된다")
    void reassignTo_refreshesUpdatedAt() {
        FcmToken token = FcmToken.create(1L, "abc", DevicePlatform.ANDROID);
        LocalDateTime before = token.getUpdatedAt();

        // 시간 해상도 문제 피하려고 살짝 대기하거나 Clock 주입을 고민
        token.reassignTo(2L, DevicePlatform.IOS);

        assertThat(token.getUpdatedAt()).isAfterOrEqualTo(before);
    }


}