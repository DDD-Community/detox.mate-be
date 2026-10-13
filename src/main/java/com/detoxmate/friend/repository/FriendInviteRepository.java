package com.detoxmate.friend.repository;

import com.detoxmate.friend.domain.FriendInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendInviteRepository extends JpaRepository<FriendInvite, Long> {

    Optional<FriendInvite> findByUserId(Long userId);

    Optional<FriendInvite> findByCode(String code);
}
