package com.detoxmate.applock.repository;

import com.detoxmate.support.UserFixtures;
import com.detoxmate.applock.domain.TimeLimit;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class TimeLimitRepositoryTest {

    @Autowired TimeLimitRepository timeLimitRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("시간 변경을 저장해도 사용자별 설정의 식별자와 생성 시각을 유지한다")
    void update_preservesSingleSettingIdentity() {
        // given
        User owner = userRepository.save(UserFixtures.createUser("owner"));
        TimeLimit saved = timeLimitRepository.saveAndFlush(TimeLimit.create(owner, 60));
        Long settingId = saved.getId();
        var createdAt = saved.getCreatedAt();
        entityManager.clear();

        // when
        TimeLimit current = timeLimitRepository.findByUser_Id(owner.getId()).orElseThrow();
        current.changeTotalLockMinutes(120);
        current.changeTotalLockMinutes(120);
        entityManager.flush();
        entityManager.clear();

        // then
        TimeLimit found = timeLimitRepository.findByUser_Id(owner.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(settingId);
        assertThat(found.getTotalLockMinutes()).isEqualTo(120);
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(countLimits(owner.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("DB는 같은 사용자에게 두 번째 시간 설정을 저장하지 못하게 한다")
    void save_rejectsDuplicateUser() {
        // given
        User owner = userRepository.save(UserFixtures.createUser("owner"));
        timeLimitRepository.saveAndFlush(TimeLimit.create(owner, 60));

        // when & then
        assertThatThrownBy(() -> timeLimitRepository.saveAndFlush(TimeLimit.create(owner, 120)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB는 존재하지 않는 사용자에게 시간 설정을 저장하지 못하게 한다")
    void insert_rejectsNonexistentUser() {
        assertThatThrownBy(() -> insertLimit(Long.MAX_VALUE, 60))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 1441})
    @DisplayName("DB는 필수 시간 값과 0분부터 1440분까지의 범위를 보장한다")
    void insert_rejectsInvalidMinutes(Integer minutes) {
        // given
        User owner = userRepository.saveAndFlush(UserFixtures.createUser("owner"));

        // when & then
        assertThatThrownBy(() -> insertLimit(owner.getId(), minutes))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("사용자 행을 물리 삭제하면 해당 설정만 함께 삭제된다")
    void deleteUser_removesOnlyOwnersTimeLimit() {
        // given
        User owner = userRepository.save(UserFixtures.createUser("owner"));
        User other = userRepository.save(UserFixtures.createUser("other"));
        timeLimitRepository.save(TimeLimit.create(owner, 60));
        timeLimitRepository.saveAndFlush(TimeLimit.create(other, 120));
        entityManager.clear();

        // when
        userRepository.deleteById(owner.getId());
        userRepository.flush();
        entityManager.clear();

        // then
        assertThat(timeLimitRepository.findByUser_Id(owner.getId())).isEmpty();
        assertThat(timeLimitRepository.findByUser_Id(other.getId())).isPresent();
        assertThat(userRepository.findById(other.getId())).isPresent();
    }

    private void insertLimit(Long userId, Integer minutes) {
        jdbcTemplate.update("""
                INSERT INTO time_limits (user_id, total_lock_minutes, created_at, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, userId, minutes);
    }

    private long countLimits(Long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM time_limits WHERE user_id = ?", Long.class, userId);
    }
}
