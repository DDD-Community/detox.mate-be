package com.detoxmate.group.service;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupChallengeSummaryResponse;
import com.detoxmate.group.dto.GroupMemberResponse;
import com.detoxmate.group.dto.GroupResponse;
import com.detoxmate.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberService groupMemberService;
    private final GroupChallengeService groupChallengeService;
    private final GroupChallengeParticipantService groupChallengeParticipantService;

    @Transactional
    public GroupResponse createGroup(Long creatorUserId, String groupName) {
        Group createdGroup = this.saveGroup(groupName);
        GroupMember groupMember = groupMemberService.saveGroupOwner(creatorUserId, createdGroup.getId());
        GroupChallenge groupChallenge = groupChallengeService.saveGroupChallenge(groupMember.getGroupId());
        groupChallengeParticipantService.saveGroupChallengeParticipant(groupMember.getId(), groupChallenge.getId());

        List<GroupMemberResponse> members = groupMemberService.getGroupMembers(createdGroup.getId());

        return toGroupResponse(createdGroup, groupMember, groupChallenge, members);
    }

    public Group saveGroup(String groupName) {
        return groupRepository.save(Group.createNew(groupName));
    }

    @Transactional
    public GroupResponse joinGroup(String inviteCode, Long userId) {
        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalStateException("초대코드에 해당하는 그룹이 없습니다."));

        if (groupMemberService.existsActiveGroupMember(userId)) {
            throw new IllegalStateException("이미 그룹이 있어서, 새로운 그룹에 참여할 수 없습니다.");
        }

        GroupChallenge groupChallenge = groupChallengeService.getLatestChallenge(group.getId());

        if (groupChallenge.getStatus().name().equals("ACTIVE")) {
            throw new IllegalStateException("챌린지가 이미 진행 중이라서, 그룹에 참여할 수 없습니다");
        }

        GroupMember groupMember = groupMemberService.saveGroupMember(userId, group.getId());
        groupChallengeParticipantService.saveGroupChallengeParticipant(groupMember.getId(), groupChallenge.getId());

        List<GroupMemberResponse> members = groupMemberService.getGroupMembers(group.getId());

        return toGroupResponse(group, groupMember, groupChallenge, members);
    }

    private GroupResponse toGroupResponse(
            Group group,
            GroupMember currentMember,
            GroupChallenge groupChallenge,
            List<GroupMemberResponse> members
    ) {
        return new GroupResponse(
                group.getId(),
                group.getInviteCode(),
                group.getName(),
                currentMember.getRole(),
                members,
                new GroupChallengeSummaryResponse(
                        groupChallenge.getId(),
                        groupChallenge.getChallengeNo(),
                        groupChallenge.getStatus().name(),
                        groupChallenge.getStartAt(),
                        groupChallenge.getEndAt()
                ),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
