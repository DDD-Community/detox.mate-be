package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.NotificationType;
import com.detoxmate.notification.domain.NotificationTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTypeRepository extends JpaRepository<NotificationType,Long> {
    Optional<NotificationType> findByTypeCode(NotificationTypeCode code);
}
