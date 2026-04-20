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

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberService groupMemberService;
    private final GroupChallengeService groupChallengeService;
    private final GroupChallengeParticipantService groupChallengeParticipantService;

    public GroupResponse createGroup(Long creatorUserId, String groupName) {
        Group createdGroup = this.saveGroup(groupName);
        GroupMember groupMember = groupMemberService.saveGroupOwner(creatorUserId, createdGroup.getId());
        GroupChallenge groupChallenge = groupChallengeService.saveGroupChallenge(groupMember.getGroupId());
        groupChallengeParticipantService.saveGroupChallengeParticipant(groupMember.getId(), groupChallenge.getId());

        List<GroupMemberResponse> members = groupMemberService.getGroupMembers(createdGroup.getId());

        return new GroupResponse(
                createdGroup.getId(),
                createdGroup.getInviteCode(),
                createdGroup.getName(),
                groupMember.getRole(),
                members,
                new GroupChallengeSummaryResponse(
                        groupChallenge.getId(),
                        groupChallenge.getChallengeNo(),
                        groupChallenge.getStatus(),
                        groupChallenge.getStartAt(),
                        groupChallenge.getEndAt()
                ),
                createdGroup.getCreatedAt(),
                createdGroup.getUpdatedAt()
        );
    }

    public Group saveGroup(String groupName) {
        return groupRepository.save(Group.createNew(groupName));
    }
}
