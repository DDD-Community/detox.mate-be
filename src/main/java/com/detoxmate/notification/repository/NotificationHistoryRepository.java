package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.domain.NotificationTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory,Long> {

    @Query("""
            SELECT h FROM NotificationHistory h
            WHERE h.userId = :userId
                AND(h.expiredAt IS NULL OR h.expiredAt>:now)
            ORDER BY h.createdAt DESC 
            """)
    List<NotificationHistory> findActiveByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Query("""
           SELECT COUNT(h) FROM NotificationHistory h
           WHERE h.userId = :userId
           AND h.isRead = false
           AND (h.expiredAt IS NULL OR h.expiredAt > :now)
          """)
    Long countUnreadActiveByUserId(@Param("userId") Long userId,
                             @Param("now") LocalDateTime now);

}
