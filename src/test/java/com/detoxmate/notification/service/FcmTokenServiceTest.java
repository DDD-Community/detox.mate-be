package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@Transactional
@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock
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
    @DisplayName("같은 토큰이 이미 존재하면 소유자가 새 유저로 갱신된다")
    void register_reassignsOwnerWhenTokenExists() {
        // given: 기존 소유자가 999L 인 토큰
        FcmToken existing = FcmToken.create(999L, "test-token-abc", DevicePlatform.ANDROID);
        given(fcmTokenRepository.findByToken("test-token-abc"))
                .willReturn(Optional.of(existing));

        // when
        FcmToken result = fcmTokenService.register(2L, "test-token-abc", DevicePlatform.IOS);

        // then: 돌아온 토큰의 상태만 확인
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getPlatform()).isEqualTo(DevicePlatform.IOS);
        assertThat(result.getToken()).isEqualTo("test-token-abc");
    }

    @Test
    @DisplayName("같은 토큰이 없으면 새로 저장된다")
    void register_insertsWhenTokenNotExists() {
        // given
        given(fcmTokenRepository.findByToken("test-token-abc"))
                .willReturn(Optional.empty());
        given(fcmTokenRepository.save(any(FcmToken.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        FcmToken result = fcmTokenService.register(2L, "test-token-abc", DevicePlatform.IOS);

        // then
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getToken()).isEqualTo("test-token-abc");
        assertThat(result.getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    @DisplayName("토큰을 삭제할 수 있다")
    void removeToken(){
        //when
        fcmTokenService.remove(1L,"test-token-abc");

        //then
        verify(fcmTokenRepository).deleteByUserIdAndToken(1L,"test-token-abc");
    }

}