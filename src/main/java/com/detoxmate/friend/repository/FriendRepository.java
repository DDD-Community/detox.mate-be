package com.detoxmate.friend.repository;

import com.detoxmate.friend.domain.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    @Query("""
            SELECT f
            FROM Friend f
            WHERE (f.fromUserId = :firstUserId AND f.toUserId = :secondUserId)
               OR (f.fromUserId = :secondUserId AND f.toUserId = :firstUserId)
            """)
    Optional<Friend> findByUserPair(
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId
    );

    @Query("""
            SELECT f
            FROM Friend f
            WHERE f.fromUserId = :userId
              AND f.status = com.detoxmate.friend.domain.FriendStatus.PENDING
            ORDER BY f.createdAt DESC, f.id DESC
            """)
    List<Friend> findSentPendingRequests(@Param("userId") Long userId);

    @Query("""
            SELECT f
            FROM Friend f
            WHERE f.toUserId = :userId
              AND f.status = com.detoxmate.friend.domain.FriendStatus.PENDING
            ORDER BY f.createdAt DESC, f.id DESC
            """)
    List<Friend> findReceivedPendingRequests(@Param("userId") Long userId);

    @Query("""
            SELECT f
            FROM Friend f
            WHERE f.status = com.detoxmate.friend.domain.FriendStatus.ACCEPTED
              AND (f.fromUserId = :userId OR f.toUserId = :userId)
            ORDER BY f.acceptedAt DESC, f.id DESC
            """)
    List<Friend> findAcceptedFriendshipsByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Friend f
            SET f.status = com.detoxmate.friend.domain.FriendStatus.ACCEPTED,
                f.acceptedAt = :acceptedAt,
                f.updatedAt = :acceptedAt
            WHERE f.id = :requestId
              AND f.toUserId = :userId
              AND f.status = com.detoxmate.friend.domain.FriendStatus.PENDING
            """)
    int acceptPendingRequest(
            @Param("requestId") Long requestId,
            @Param("userId") Long userId,
            @Param("acceptedAt") LocalDateTime acceptedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM Friend f
            WHERE f.id = :requestId
              AND f.status = com.detoxmate.friend.domain.FriendStatus.PENDING
              AND (f.fromUserId = :userId OR f.toUserId = :userId)
            """)
    int deletePendingRequest(
            @Param("requestId") Long requestId,
            @Param("userId") Long userId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM Friend f
            WHERE f.id = :friendshipId
              AND f.status = com.detoxmate.friend.domain.FriendStatus.ACCEPTED
              AND (f.fromUserId = :userId OR f.toUserId = :userId)
            """)
    int deleteAcceptedFriend(
            @Param("friendshipId") Long friendshipId,
            @Param("userId") Long userId
    );
}
