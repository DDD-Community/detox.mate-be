package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.NotificationType;
import com.detoxmate.notification.domain.NotificationTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NotificationTypeRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificationTypeRepository notificationTypeRepository;

    @Test
    @DisplayName("알림 타입을 저장하고 ID로 조회한다.")
    void saveAndFindById(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.COMMENT_CREATED);

        //when
        NotificationType saved = notificationTypeRepository.save(type);
        em.flush();
        em.clear();
        Optional<NotificationType> found = notificationTypeRepository.findById(saved.getId());

        //then
        assertThat(found).isPresent();
        assertThat(found.get().getTypeCode()).isEqualTo(NotificationTypeCode.COMMENT_CREATED);
    }

    @Test
    @DisplayName("알림 타입 코드로 알림 타입을 조회한다.")
    void findByCode(){
        //given
        em.persist(NotificationType.create(NotificationTypeCode.COMMENT_CREATED));
        em.persist(NotificationType.create(NotificationTypeCode.REACTION_CREATED));
        em.flush();
        em.clear();

        //when
        Optional<NotificationType> found = notificationTypeRepository.findByTypeCode(NotificationTypeCode.COMMENT_CREATED);

        //then
        assertThat(found).isPresent();
        assertThat(found.get().getTypeCode()).isEqualTo(NotificationTypeCode.COMMENT_CREATED);

    }

    @Test
    @DisplayName("존재하지 않는 코드로 조회하면 빈 Optional을 반환한다.")
    void findByCodeNotExists(){
        //given
        em.persist(NotificationType.create(NotificationTypeCode.COMMENT_CREATED));
        em.flush();

        //when
        Optional<NotificationType> found= notificationTypeRepository.findByTypeCode(NotificationTypeCode.REACTION_CREATED);

        //then
        assertThat(found).isEmpty();
    }
}
