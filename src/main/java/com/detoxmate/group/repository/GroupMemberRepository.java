package com.detoxmate.group.repository;

import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    @Query("""
            SELECT new com.detoxmate.group.dto.GroupMemberResponse(
                gm.id,
                gm.userId,
                u.displayName,
                u.profileImageUrl,
                gm.role,
                gm.status,
                gm.joinedAt,
                gm.leftAt
            )
            FROM GroupMember gm
            LEFT JOIN User u ON gm.userId = u.id
            WHERE gm.groupId = :groupId
            """)
    List<GroupMemberResponse> findMembersWithUserByGroupId(@Param("groupId") Long groupId);
}
