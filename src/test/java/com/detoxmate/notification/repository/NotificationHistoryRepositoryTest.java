package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.Notification;
import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.domain.NotificationType;
import com.detoxmate.notification.domain.NotificationTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class NotificationHistoryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificationHistoryRepository historyRepository;

    private Notification persistTemplate(NotificationTypeCode code, String title, String template){
        NotificationType type = em.persist(NotificationType.create(code));
        return em.persist(Notification.create(type, title, template));
    }

    @Test
    @DisplayName("알림 이력을 저장하고 연관된 notification과 함께 조회한다.")
    void saveAndFind(){
        //given
        Notification notification = persistTemplate(
                NotificationTypeCode.COMMENT,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );
        NotificationHistory history = NotificationHistory.from(notification,1L,"xeulbn");

        //when
        NotificationHistory saved = historyRepository.save(history);
        em.flush();
        em.clear();
        NotificationHistory found = historyRepository.findById(saved.getId()).orElseThrow();

        //then
        assertThat(found.getUserId()).isEqualTo(1L);
        assertThat(found.getMessage()).isEqualTo("xeulbn님이 댓글을 남겼습니다");
        assertThat(found.getNotification().getTitle()).isEqualTo("댓글 알림");

    }

    @Test
    @DisplayName("사용자의 만료되지 않은 알림만 최신순으로 조회한다.")
    void findActiveByUserId() {
        // given
        Notification notification = persistTemplate(
                NotificationTypeCode.COMMENT,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );
        Long myId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 4, 21, 12, 0);

        // createdAt을 만들어놓기
        NotificationHistory older = NotificationHistory.from(notification, myId, "A");
        setCreatedAt(older, now.minusHours(2));
        em.persist(older);

        NotificationHistory newer = NotificationHistory.from(notification, myId, "B", now.plusDays(1));
        setCreatedAt(newer, now.minusHours(1));
        em.persist(newer);

        // 제외 대상들 (만료 & 타인)
        em.persist(NotificationHistory.from(notification, myId, "C", now.minusDays(1)));
        em.persist(NotificationHistory.from(notification, 2L, "D"));

        em.flush();
        em.clear();

        // when
        List<NotificationHistory> results = historyRepository.findActiveByUserId(myId, now);

        // then : 내용 & 순서를 한 번에
        assertThat(results)
                .extracting(NotificationHistory::getMessage)
                .containsExactly(
                        "B님이 댓글을 남겼습니다",
                        "A님이 댓글을 남겼습니다"
                );
    }


    @Test
    @DisplayName("사용자의 만료되지 않은 읽지 않은 알림 개수만 조회한다.")
    void countUnreadActiveByUserId() {
        // given
        Notification notification = persistTemplate(
                NotificationTypeCode.COMMENT,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );
        Long myId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 4, 21, 12, 0);

        // 읽음
        NotificationHistory read = NotificationHistory.from(notification, myId, "A");
        read.markAsRead();
        historyRepository.save(read);

        // 안 읽음, 미만료
        historyRepository.save(NotificationHistory.from(notification, myId, "B"));
        historyRepository.save(NotificationHistory.from(
                notification, myId, "C", now.plusDays(1)));

        // 안 읽음, 만료됨
        historyRepository.save(NotificationHistory.from(
                notification, myId, "D", now.minusDays(1)));

        // 다른 유저
        historyRepository.save(NotificationHistory.from(notification, 2L, "E"));
        em.flush();

        // when
        Long unread = historyRepository.countUnreadActiveByUserId(myId, now);

        // then
        assertThat(unread).isEqualTo(2);
    }

    @Test
    @DisplayName("만료 시각이 현재 시각과 정확히 같으면 만료된 것으로 간주한다.")
    void findActiveByUserId_whenExpiredAtEqualsNow_excludes() {
        // given
        Notification notification = persistTemplate(
                NotificationTypeCode.COMMENT,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );
        Long myId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 4, 21, 12, 0);

        // 만료 시각 == 현재 시각
        em.persist(NotificationHistory.from(notification, myId, "A", now));
        em.flush();
        em.clear();

        // when
        List<NotificationHistory> results = historyRepository.findActiveByUserId(myId, now);

        // then
        assertThat(results).isEmpty();
    }


    private void setCreatedAt(NotificationHistory h, LocalDateTime t) {
        ReflectionTestUtils.setField(h, "createdAt", t);
    }

}