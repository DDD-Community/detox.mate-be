package com.detoxmate.applock.repository;

import com.detoxmate.applock.domain.App;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AppRepositoryTest {

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("App을 저장하면 소유된 AppTimeLimit 하나가 함께 저장되고 사용자 범위로 조회된다")
    void save_persistsOneOwnedTimeLimitAndFindsValuesByUser() {
        // given
        User owner = userRepository.save(User.createNew("owner"));
        User other = userRepository.save(User.createNew("other"));
        App saved = appRepository.save(App.create(owner, "Instagram", 60));
        appRepository.save(App.create(other, "Other App", 120));
        Long appId = saved.getId();

        entityManager.flush();
        entityManager.clear();

        // when
        List<App> ownersApps = appRepository.findAllByUser_Id(owner.getId());

        // then
        assertThat(ownersApps).hasSize(1);
        App found = ownersApps.get(0);
        assertThat(found.getId()).isEqualTo(appId);
        assertThat(found.getUser().getId()).isEqualTo(owner.getId());
        assertThat(found.getAppDisplayName()).isEqualTo("Instagram");
        assertThat(found.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(60);
        assertThat(countTimeLimitsForApp(appId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("App aggregate를 수정하면 같은 App과 AppTimeLimit row가 갱신된다")
    void update_preservesAggregateIdsAndSingleOwnedTimeLimit() {
        // given
        User owner = userRepository.save(User.createNew("owner"));
        App saved = appRepository.save(App.create(owner, "Instagram", 60));
        entityManager.flush();

        Long appId = saved.getId();
        Long appTimeLimitId = saved.getAppTimeLimit().getId();
        entityManager.clear();

        App persisted = appRepository.findByIdAndUser_Id(appId, owner.getId()).orElseThrow();

        // when
        persisted.update("YouTube", 120);
        entityManager.flush();
        entityManager.clear();

        // then
        App updated = appRepository.findByIdAndUser_Id(appId, owner.getId()).orElseThrow();
        assertThat(updated.getId()).isEqualTo(appId);
        assertThat(updated.getAppDisplayName()).isEqualTo("YouTube");
        assertThat(updated.getAppTimeLimit().getId()).isEqualTo(appTimeLimitId);
        assertThat(updated.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(120);
        assertThat(countTimeLimitsForApp(appId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("App aggregate를 동일한 값으로 수정해도 같은 App과 AppTimeLimit row 하나를 유지한다")
    void updateWithIdenticalValues_preservesAggregateIdsAndSingleOwnedTimeLimit() {
        // given
        User owner = userRepository.save(User.createNew("owner"));
        App saved = appRepository.save(App.create(owner, "Instagram", 60));
        entityManager.flush();

        Long appId = saved.getId();
        Long appTimeLimitId = saved.getAppTimeLimit().getId();
        entityManager.clear();

        App persisted = appRepository.findByIdAndUser_Id(appId, owner.getId()).orElseThrow();

        // when
        persisted.update("Instagram", 60);
        entityManager.flush();
        entityManager.clear();

        // then
        App updated = appRepository.findByIdAndUser_Id(appId, owner.getId()).orElseThrow();
        assertThat(updated.getId()).isEqualTo(appId);
        assertThat(updated.getAppDisplayName()).isEqualTo("Instagram");
        assertThat(updated.getAppTimeLimit().getId()).isEqualTo(appTimeLimitId);
        assertThat(updated.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(60);
        assertThat(countTimeLimitsForApp(appId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("App을 삭제하면 소유된 AppTimeLimit은 삭제되고 User는 보존된다")
    void delete_removesOwnedTimeLimitAndPreservesUser() {
        // given
        User owner = userRepository.save(User.createNew("owner"));
        App saved = appRepository.save(App.create(owner, "Instagram", 60));
        entityManager.flush();

        Long userId = owner.getId();
        Long appId = saved.getId();
        entityManager.clear();

        App persisted = appRepository.findByIdAndUser_Id(appId, userId).orElseThrow();

        // when
        appRepository.delete(persisted);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(appRepository.findById(appId)).isEmpty();
        assertThat(countTimeLimitsForApp(appId)).isZero();
        assertThat(userRepository.findById(userId)).isPresent();
    }

    private long countTimeLimitsForApp(Long appId) {
        Number count = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM app_time_limits
                        WHERE app_id = :appId
                        """)
                .setParameter("appId", appId)
                .getSingleResult();
        return count.longValue();
    }
}
