package com.detoxmate.notification.repository;

import com.detoxmate.notification.domain.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);
    List<FcmToken> findAllByUserId(Long userId);
    void deleteByUserIdAndToken(Long userId, String token);
    void deleteByToken(String token);
    void deleteAllByTokenIn(Collection<String> tokens);

}
