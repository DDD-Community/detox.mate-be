package com.detoxmate.applock.service;

import com.detoxmate.support.UserFixtures;
import com.detoxmate.applock.domain.TimeLimit;
import com.detoxmate.applock.repository.TimeLimitRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(TimeLimitService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TimeLimitServiceTest {

    @Autowired TimeLimitService timeLimitService;
    @Autowired TimeLimitRepository timeLimitRepository;
    @Autowired UserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.saveAndFlush(UserFixtures.createUser("time-limit-owner")).getId();
    }

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            timeLimitRepository.findByUser_Id(userId).ifPresent(timeLimitRepository::delete);
            timeLimitRepository.flush();
            userRepository.deleteById(userId);
        });
    }

    @Test
    @DisplayName("동시에 처음 저장해도 두 요청 모두 성공하고 사용자 설정 한 행만 남는다")
    void concurrentFirstSet_keepsOneCurrentSetting() throws Exception {
        // given
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> setTogether(60, ready, start));
            var second = executor.submit(() -> setTogether(120, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // when
            start.countDown();

            // then
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(60);
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(120);
            List<TimeLimit> settings = timeLimitRepository.findAll();
            assertThat(settings).filteredOn(setting -> setting.getUser().getId().equals(userId)).hasSize(1);
            assertThat(timeLimitService.get(userId)).isIn(60, 120);
        } finally {
            start.countDown();
        }
    }

    @Test
    @DisplayName("잘못된 시간으로 변경한 트랜잭션이 실패해도 이전 설정은 유지된다")
    void set_preservesStoredValueAfterInvalidUpdate() {
        // given
        timeLimitService.set(userId, 60);

        // when & then
        assertThatIllegalArgumentException().isThrownBy(() -> timeLimitService.set(userId, -1));
        assertThat(timeLimitService.get(userId)).isEqualTo(60);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 잠금 설정을 변경하거나 삭제할 수 없다")
    void writes_rejectWithdrawnUserAndPreserveSetting() {
        // given
        timeLimitService.set(userId, 60);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                userRepository.findByIdForUpdate(userId).orElseThrow().withdraw());

        // when & then
        assertThatThrownBy(() -> timeLimitService.set(userId, 120))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        failure -> assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> timeLimitService.delete(userId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        failure -> assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThat(timeLimitRepository.findByUser_Id(userId).orElseThrow().getTotalLockMinutes()).isEqualTo(60);
    }

    private int setTogether(int minutes, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 저장 시작 신호를 받지 못했습니다.");
        }
        return timeLimitService.set(userId, minutes);
    }
}
