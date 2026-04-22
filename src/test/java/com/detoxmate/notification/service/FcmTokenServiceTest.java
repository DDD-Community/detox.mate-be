package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @MockitoBean
    private FcmTokenRepository fcmTokenRepository;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    @Test
    @DisplayName("토큰을 등록할 수 있다")
    void registerToken(){
        //given
        given(fcmTokenRepository.save(any(FcmToken.class)))
                .willAnswer(inv -> inv.getArgument(0));

        //when
        FcmToken result = fcmTokenService.register(1L, "test-token-abc", DevicePlatform.IOS);

        //then
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getToken()).isEqualTo("test-token-abc");
        assertThat(result.getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    @DisplayName("등록 시 기존 동일 토큰은 먼저 삭제된다")
    void registerDeletesExistingToken(){
        //given
        given(fcmTokenRepository.save(any(FcmToken.class)))
                .willAnswer(inv -> inv.getArgument(0));

        //when
        fcmTokenService.register(2L,"test-token-abc",DevicePlatform.IOS);

        //then
        InOrder inOrder = Mockito.inOrder(fcmTokenRepository);
        inOrder.verify(fcmTokenRepository).deleteByToken("test-token-abc");
        inOrder.verify(fcmTokenRepository).save(any(FcmToken.class));
    }

    @Test
    @DisplayName("토큰을 삭제할 수 있다")
    void removeToken(){
        //when
        fcmTokenService.remove("test-token-abc");

        //then
        verify(fcmTokenRepository).deleteByToken("test-token-abc");
    }

}