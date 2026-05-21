package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class FcmTokenRepositoryTest {

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Test
    @DisplayName("Fcm Token을 저장하고 ID로 조회할 수 있다")
    void saveAndFindById(){
        //given
        String tokenValue = "test-token-abc-123";
        FcmToken token = FcmToken.create(1L, tokenValue, DevicePlatform.IOS);

        //when
        FcmToken saved = fcmTokenRepository.save(token);
        Optional<FcmToken> found = fcmTokenRepository.findById(saved.getId());

        //then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo(tokenValue);
        assertThat(found.get().getUserId()).isEqualTo(1L);
        assertThat(found.get().getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    @DisplayName("토큰 문자열로 FCM 토큰을 조회할 수 있다")
    void findByToken(){
        //given
        FcmToken token =FcmToken.create(1L,"test-token-abc-123",DevicePlatform.IOS);
        FcmToken saved = fcmTokenRepository.save(token);

        //when
        Optional<FcmToken> found = fcmTokenRepository.findByToken("test-token-abc-123");

        //then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 토큰 조회시 Optional.empty를 반환한다.")
    void findByTokenNotExists(){
        //when
        Optional<FcmToken> found = fcmTokenRepository.findByToken("non-existent");

        //then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("한 사용자의 모든 디바이스 토큰을 조회할 수 있다")
    void findAllByUserId() {
        // given
        FcmToken android = FcmToken.create(1L, "token-android", DevicePlatform.ANDROID);
        FcmToken ios = FcmToken.create(1L, "token-ios", DevicePlatform.IOS);
        FcmToken otherUser = FcmToken.create(2L, "token-other", DevicePlatform.ANDROID);
        fcmTokenRepository.save(android);
        fcmTokenRepository.save(ios);
        fcmTokenRepository.save(otherUser);

        // when
        List<FcmToken> tokens = fcmTokenRepository.findAllByUserId(1L);

        // then
        assertThat(tokens)
                .extracting(FcmToken::getToken)
                .containsExactlyInAnyOrder("token-android", "token-ios");
    }

    @Test
    @DisplayName("토큰이 없는 사용자 조회시 빈 리스트를 반환한다")
    void findAllByUserIdEmpty(){
        //when
        List<FcmToken> tokens = fcmTokenRepository.findAll();

        //then
        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID와 토큰으로 FCM 토큰을 삭제할 수 있다")
    void deleteByUserIdAndToken(){
        //given
        FcmToken token = FcmToken.create(1L,"test-token",DevicePlatform.IOS);
        fcmTokenRepository.save(token);

        //when
        fcmTokenRepository.deleteByUserIdAndToken(1L,"test-token");

        //then
        assertThat(fcmTokenRepository.findByToken("test-token")).isEmpty();
    }

    @Test
    @DisplayName("소유자가 다르면 deleteByUserIdAndToken은 아무것도 지우지 않는다")
    void deleteByUserIdAndToken_doesNothing_whenOwnerMismatch() {
        FcmToken saved = FcmToken.create(1L, "token-A", DevicePlatform.IOS);
        fcmTokenRepository.save(saved);

        fcmTokenRepository.deleteByUserIdAndToken(2L, "token-A"); // 다른 유저가 지우려 시도

        assertThat(fcmTokenRepository.findByToken("token-A")).isPresent();
    }

    @Test
    @DisplayName("bulk token delete는 이미 삭제된 토큰을 다시 삭제해도 예외 없이 0을 반환한다")
    void deleteByTokenInBulk_isIdempotent() {
        // given
        fcmTokenRepository.save(FcmToken.create(1L, "dead-token", DevicePlatform.IOS));

        // when
        int firstDeletedCount = fcmTokenRepository.deleteByTokenInBulk(List.of("dead-token"));
        int secondDeletedCount = fcmTokenRepository.deleteByTokenInBulk(List.of("dead-token"));

        // then
        assertThat(firstDeletedCount).isEqualTo(1);
        assertThat(secondDeletedCount).isZero();
        assertThat(fcmTokenRepository.findByToken("dead-token")).isEmpty();
    }
}
