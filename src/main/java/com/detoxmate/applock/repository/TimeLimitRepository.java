package com.detoxmate.applock.repository;

import com.detoxmate.applock.domain.TimeLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimeLimitRepository extends JpaRepository<TimeLimit, Long> {

    Optional<TimeLimit> findByUser_Id(Long userId);
}
