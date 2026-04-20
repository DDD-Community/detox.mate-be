package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.NotificationType;
import com.detoxmate.notification.domain.NotificationTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class NotificationTypeRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificationTypeRepository notificationTypeRepository;

    @Test
    @DisplayName("알림 타입을 저장하고 ID로 조회한다.")
    void saveAndFindById(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.COMMENT);

        //when
        NotificationType saved = notificationTypeRepository.save(type);
        em.flush();
        em.clear();
        Optional<NotificationType> found = notificationTypeRepository.findById(saved.getId());

        //then
        assertThat(found).isPresent();
        assertThat(found.get().getTypeCode()).isEqualTo(NotificationTypeCode.COMMENT);
    }

    @Test
    @DisplayName("알림 타입 코드로 알림 타입을 조회한다.")
    void findByCode(){
        //given
        em.persist(NotificationType.create(NotificationTypeCode.COMMENT));
        em.persist(NotificationType.create(NotificationTypeCode.REACTION));
        em.flush();
        em.clear();

        //when
        Optional<NotificationType> found = notificationTypeRepository.findByCode(NotificationTypeCode.COMMENT);

        //then
        assertThat(found).isPresent();
        assertThat(found.get().getTypeCode()).isEqualTo(NotificationTypeCode.COMMENT);

    }

    @Test
    @DisplayName("존재하지 않는 코드로 조회하면 빈 Optional을 반환한다.")
    void findByCodeNotExists(){
        //given
        em.persist(NotificationType.create(NotificationTypeCode.COMMENT));
        em.flush();

        //when
        Optional<NotificationType> found= notificationTypeRepository.findByCode(NotificationTypeCode.REACTION);

        //then
        assertThat(found).isEmpty();
    }
}