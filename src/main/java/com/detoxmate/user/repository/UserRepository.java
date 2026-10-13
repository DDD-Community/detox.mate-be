package com.detoxmate.user.repository;

import com.detoxmate.user.domain.User;
import com.detoxmate.user.domain.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.userCode = :userCode
              AND (u.status IS NULL OR u.status = :activeStatus)
            """)
    Optional<User> findActiveByUserCode(
            @Param("userCode") String userCode,
            @Param("activeStatus") UserStatus activeStatus
    );

    Optional<User> findByEmail(String email);
}
