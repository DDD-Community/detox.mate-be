package com.detoxmate.group.repository;

import com.detoxmate.group.domain.GroupChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupChallengeRepository extends JpaRepository<GroupChallenge, Long> {
    Optional<GroupChallenge> findTopByGroupIdOrderByChallengeNoDesc(Long groupId);
}
