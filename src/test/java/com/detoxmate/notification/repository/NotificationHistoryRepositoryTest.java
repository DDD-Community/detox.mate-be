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
                NotificationTypeCode.COMMENT_CREATED,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );
        NotificationHistory history = createHistory(notification, 1L, "xeulbn");

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
                NotificationTypeCode.FRIEND_REQUEST_RECEIVED,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );
        Long myId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 4, 21, 12, 0);

        // createdAt을 만들어놓기
        NotificationHistory older = createHistory(notification, myId, "A");
        setCreatedAt(older, now.minusHours(2));
        em.persist(older);

        NotificationHistory newer = createHistory(notification, myId, "B", now.plusDays(1));
        setCreatedAt(newer, now.minusHours(1));
        em.persist(newer);

        // 제외 대상들 (만료 & 타인)
        em.persist(createHistory(notification, myId, "C", now.minusDays(1)));
        em.persist(createHistory(notification, 2L, "D"));

        em.flush();
        em.clear();

        // when
        List<NotificationHistory> results = historyRepository.findActiveByUserId(myId, now, now.minusHours(168), NotificationTypeCode.FRIEND_HISTORY_TYPES);

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
                NotificationTypeCode.COMMENT_CREATED,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );
        Long myId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 4, 21, 12, 0);

        // 읽음
        NotificationHistory read = createHistory(notification, myId, "A");
        read.markAsRead();
        historyRepository.save(read);

        // 안 읽음, 미만료
        historyRepository.save(createHistory(notification, myId, "B"));
        historyRepository.save(createHistory(notification, myId, "C", now.plusDays(1)));

        // 안 읽음, 만료됨
        historyRepository.save(createHistory(notification, myId, "D", now.minusDays(1)));

        // 다른 유저
        historyRepository.save(createHistory(notification, 2L, "E"));
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
                NotificationTypeCode.FRIEND_REQUEST_RECEIVED,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다"
        );
        Long myId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 4, 21, 12, 0);

        // 만료 시각 == 현재 시각
        em.persist(createHistory(notification, myId, "A", now));
        em.flush();
        em.clear();

        // when
        List<NotificationHistory> results = historyRepository.findActiveByUserId(myId, now, now.minusHours(168), NotificationTypeCode.FRIEND_HISTORY_TYPES);

        // then
        assertThat(results).isEmpty();
    }


    @Test
    void findActiveByUserId_onlyFriendTypesWithin168Hours() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 13, 15, 0);
        Notification friend = persistTemplate(NotificationTypeCode.FRIEND_REQUEST_RECEIVED, "친구", "친구");
        Notification personal = persistTemplate(NotificationTypeCode.APP_UNLOCK_REQUESTED, "본인", "본인");
        Notification legacy = persistTemplate(NotificationTypeCode.COMMENT_CREATED, "댓글", "댓글");
        NotificationHistory recent = createHistory(friend, 1L, "recent");
        setCreatedAt(recent, now.minusHours(168).plusSeconds(1));
        em.persist(recent);
        NotificationHistory boundary = createHistory(friend, 1L, "boundary");
        setCreatedAt(boundary, now.minusHours(168));
        em.persist(boundary);
        NotificationHistory old = createHistory(friend, 1L, "old");
        setCreatedAt(old, now.minusHours(169));
        em.persist(old);
        em.persist(createHistory(personal, 1L, "personal"));
        em.persist(createHistory(legacy, 1L, "legacy"));
        em.flush();
        em.clear();

        assertThat(historyRepository.findActiveByUserId(1L, now, now.minusHours(168), NotificationTypeCode.FRIEND_HISTORY_TYPES))
                .extracting(NotificationHistory::getMessage)
                .containsExactly("recent님이 댓글을 남겼습니다");
    }

    @Test
    void findActiveByUserId_includesAllFiveFriendTypes() {
        LocalDateTime now = LocalDateTime.now();
        for (NotificationTypeCode code : List.of(
                NotificationTypeCode.FRIEND_REQUEST_RECEIVED,
                NotificationTypeCode.FRIEND_REQUEST_ACCEPTED,
                NotificationTypeCode.FRIEND_APP_UNLOCKED,
                NotificationTypeCode.FRIEND_TIME_LIMIT_CHANGED,
                NotificationTypeCode.FRIEND_UNLOCKED)) {
            Notification template = persistTemplate(code, "친구", "친구");
            NotificationHistory history = createHistory(template, 1L, code.name());
            setCreatedAt(history, now.minusHours(1));
            em.persist(history);
        }
        em.flush();
        em.clear();

        assertThat(historyRepository.findActiveByUserId(1L, now, now.minusHours(168), NotificationTypeCode.FRIEND_HISTORY_TYPES))
                .hasSize(5);
    }

    private void setCreatedAt(NotificationHistory h, LocalDateTime t) {
        ReflectionTestUtils.setField(h, "createdAt", t);
    }

    private NotificationHistory createHistory(Notification notification, Long userId, String nickname) {
        return createHistory(notification, userId, nickname, null);
    }

    private NotificationHistory createHistory(
            Notification notification,
            Long userId,
            String nickname,
            LocalDateTime expiredAt
    ) {
        return NotificationHistory.fromResolvedMessage(
                notification,
                userId,
                nickname + "님이 댓글을 남겼습니다",
                expiredAt
        );
    }

}
