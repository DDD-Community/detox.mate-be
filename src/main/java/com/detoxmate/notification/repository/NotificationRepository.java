package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.Notification;
import com.detoxmate.notification.domain.NotificationTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


    @Query("SELECT n FROM Notification n WHERE n.type.typeCode = :typeCode")
    Optional<Notification> findByTypeCode(@Param("typeCode") NotificationTypeCode typeCode);
}
