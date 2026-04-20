package com.detoxmate.group.service;

import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberResponse;
import com.detoxmate.group.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMemberService {
    private final GroupMemberRepository groupMemberRepository;

    public GroupMember saveGroupMember(Long userId, Long groupId) {
        return groupMemberRepository.save(GroupMember.createMember(userId, groupId));
    }

    public GroupMember saveGroupOwner(Long userId, Long groupId) {
        return groupMemberRepository.save(GroupMember.createOwner(userId, groupId));
    }

    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        return groupMemberRepository.findMembersWithUserByGroupId(groupId);
    }

    public List<GroupMember> getActiveGroupMembers(Long userId) {
        return groupMemberRepository.findAllByUserIdAndStatus(userId, "ACTIVE");
    }

    public GroupMember getActiveGroupMember(Long userId, Long groupId) {
        return groupMemberRepository.findByUserIdAndGroupIdAndStatus(userId, groupId, "ACTIVE")
                .orElseThrow();
    }

    public boolean existsActiveGroupMember(Long userId) {
        return groupMemberRepository.existsByUserIdAndStatus(userId, "ACTIVE");
    }

    public void deleteGroupMembers(Long groupId) {
        groupMemberRepository.deleteAllByGroupId(groupId);
    }
}
