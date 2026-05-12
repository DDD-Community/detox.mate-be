package com.detoxmate.group.service;

import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberResponse;
import com.detoxmate.group.dto.GroupMemberUserQueryResult;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupMemberService {
    private final GroupMemberRepository groupMemberRepository;
    private final ImageReadUrlBuilder imageReadUrlBuilder;

    public GroupMember saveGroupMember(Long userId, Long groupId) {
        return groupMemberRepository.save(GroupMember.createMember(userId, groupId));
    }

    public GroupMember saveGroupOwner(Long userId, Long groupId) {
        return groupMemberRepository.save(GroupMember.createOwner(userId, groupId));
    }

    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        return groupMemberRepository.findMemberUserQueryResultsByGroupId(groupId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<GroupMember> getActiveGroupMembers(Long userId) {
        return groupMemberRepository.findAllByUserIdAndStatus(userId, "ACTIVE");
    }

    public GroupMember getActiveGroupMember(Long userId, Long groupId) {
        return groupMemberRepository.findByUserIdAndGroupIdAndStatus(userId, groupId, "ACTIVE")
                .orElseThrow();
    }

    public Optional<GroupMember> findActiveGroupMember(Long userId, Long groupId) {
        return groupMemberRepository.findByUserIdAndGroupIdAndStatus(userId, groupId, "ACTIVE");
    }

    public Optional<GroupMember> findLatestActiveMemberExcept(Long groupId, Long excludedGroupMemberId) {
        return groupMemberRepository.findFirstByGroupIdAndStatusAndIdNotOrderByJoinedAtDescIdDesc(
                groupId,
                "ACTIVE",
                excludedGroupMemberId
        );
    }

    public boolean existsActiveGroupMember(Long userId) {
        return groupMemberRepository.existsByUserIdAndStatus(userId, "ACTIVE");
    }

    public void deleteGroupMembers(Long groupId) {
        groupMemberRepository.deleteAllByGroupId(groupId);
    }

    public void leaveGroupMember(GroupMember groupMember) {
        groupMember.leave();
    }

    private GroupMemberResponse toResponse(GroupMemberUserQueryResult row) {
        return new GroupMemberResponse(
                row.id(),
                row.userId(),
                row.displayName(),
                imageReadUrlBuilder.build(row.profileImageObjectKey()),
                row.role(),
                row.status(),
                row.joinedAt(),
                row.leftAt(),
                row.userWithdrawn()
        );
    }

    public void promoteToOwner(GroupMember groupMember) {
        groupMember.promoteToOwner();
    }
}
