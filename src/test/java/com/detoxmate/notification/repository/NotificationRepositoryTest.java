package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.Notification;
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
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Notification을 저장하고 ID로 조회하면 저장한 값이 그대로 반환된다")
    void saveAndFindById(){
        //given
        NotificationType type = NotificationType.create(NotificationTypeCode.COMMENT_CREATED);
        em.persist(type);
        Notification notification = Notification.create(
                type,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );

        //when
        Notification saved = notificationRepository.save(notification);
        em.flush();
        em.clear();
        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();

        //then
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getTitle()).isEqualTo("댓글 알림");
        assertThat(found.getMessageTemplate()).isEqualTo("{nickname}님이 댓글을 남겼습니다");
        assertThat(found.getType().getTypeCode()).isEqualTo(NotificationTypeCode.COMMENT_CREATED);
    }

    @Test
    @DisplayName("타입 코드로 조회하면 해당 타입의 알림 템플릿을 반환한다")
    void findByTypeCode(){
        //given
        NotificationType commentType = NotificationType.create(NotificationTypeCode.COMMENT_CREATED);
        NotificationType reactionType = NotificationType.create(NotificationTypeCode.REACTION_CREATED);
        em.persist(commentType);
        em.persist(reactionType);

        em.persist(Notification.create(commentType, "댓글 알림", "{nickname}님이 댓글을 남겼습니다"));
        em.persist(Notification.create(reactionType, "반응 알림", "{nickname}님이 반응을 남겼습니다"));
        em.flush();
        em.clear();

        //when
        Optional<Notification> found = notificationRepository.findByTypeCode(NotificationTypeCode.COMMENT_CREATED);

        //then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("댓글 알림");
        assertThat(found.get().getType().getTypeCode()).isEqualTo(NotificationTypeCode.COMMENT_CREATED);
    }

    @Test
    @DisplayName("존재하지 않는 타입 코드로 조회하면 빈 Optional을 반환한다")
    void findByTypeCodeNotExists(){
        //given
        NotificationType commentType = NotificationType.create(NotificationTypeCode.COMMENT_CREATED);
        em.persist(commentType);
        em.persist(Notification.create(
                commentType,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다")
        );
        em.flush();
        em.clear();

        //when
        Optional<Notification> found = notificationRepository.findByTypeCode(NotificationTypeCode.CERTIFICATION_CREATED);

        // then
        assertThat(found).isEmpty();

    }

}
