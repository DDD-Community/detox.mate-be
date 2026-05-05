package com.detoxmate.group.repository;

import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberUserQueryResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByUserId(Long userId);

    Optional<GroupMember> findByUserIdAndGroupIdAndStatus(Long userId, Long groupId, String status);

    List<GroupMember> findAllByUserIdAndStatus(Long userId, String status);

    Optional<GroupMember> findFirstByGroupIdAndStatusAndIdNotOrderByJoinedAtDescIdDesc(
            Long groupId,
            String status,
            Long excludedGroupMemberId
    );

    void deleteAllByGroupId(Long groupId);

    boolean existsByUserIdAndStatus(Long userId, String status);

    @Query("""
            SELECT new com.detoxmate.group.dto.GroupMemberUserQueryResult(
                gm.id,
                gm.userId,
                u.displayName,
                u.profileImageObjectKey,
                gm.role,
                gm.status,
                gm.joinedAt,
                gm.leftAt
            )
            FROM GroupMember gm
            LEFT JOIN User u ON gm.userId = u.id
            WHERE gm.groupId = :groupId
              AND gm.status = 'ACTIVE'
            ORDER BY gm.joinedAt ASC
            """)
    List<GroupMemberUserQueryResult> findMemberUserQueryResultsByGroupId(@Param("groupId") Long groupId);
}
