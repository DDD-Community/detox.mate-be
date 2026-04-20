package com.detoxmate.group.service;

import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
