package com.detoxmate.group.service;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeStatus;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupChallengeSummaryResponse;
import com.detoxmate.group.dto.GroupMemberResponse;
import com.detoxmate.group.dto.GroupResponse;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GroupService {

    private static final int MAX_INVITE_CODE_GENERATION_ATTEMPTS = 10;

    private final GroupRepository groupRepository;
    private final GroupMemberService groupMemberService;
    private final GroupChallengeService groupChallengeService;
    private final GroupChallengeParticipantService groupChallengeParticipantService;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponse createGroup(Long creatorUserId, String groupName) {
        lockUserForGroupOperation(creatorUserId);

        if (groupMemberService.existsActiveGroupMember(creatorUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 그룹이 있어서, 새로운 그룹을 생성할 수 없습니다.");
        }

        Group createdGroup = saveGroupWithUniqueInviteCode(groupName);
        GroupMember groupMember = groupMemberService.saveGroupOwner(creatorUserId, createdGroup.getId());
        GroupChallenge groupChallenge = groupChallengeService.saveGroupChallenge(groupMember.getGroupId());
        groupChallengeParticipantService.saveGroupChallengeParticipant(groupMember.getId(), groupChallenge.getId());

        List<GroupMemberResponse> members = groupMemberService.getGroupMembers(createdGroup.getId());

        return toGroupResponse(createdGroup, groupMember, groupChallenge, members);
    }

    public Group saveGroup(String groupName, String inviteCode) {
        return groupRepository.save(Group.createNew(groupName, inviteCode));
    }

    @Transactional
    public GroupResponse joinGroup(String inviteCode, Long userId) {
        lockUserForGroupOperation(userId);

        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대코드에 해당하는 그룹이 없습니다."));

        if (groupMemberService.existsActiveGroupMember(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 그룹이 있어서, 새로운 그룹에 참여할 수 없습니다.");
        }

        GroupChallenge groupChallenge = groupChallengeService.getLatestChallenge(group.getId());

        if (groupChallenge.getStatus() != GroupChallengeStatus.RECRUITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 참여 가능한 그룹 챌린지가 없습니다.");
        }

        GroupMember groupMember = groupMemberService.saveGroupMember(userId, group.getId());
        groupChallengeParticipantService.saveGroupChallengeParticipant(groupMember.getId(), groupChallenge.getId());

        List<GroupMemberResponse> members = groupMemberService.getGroupMembers(group.getId());

        return toGroupResponse(group, groupMember, groupChallenge, members);
    }

    public List<GroupResponse> getMyGroups(Long userId) {
        return groupMemberService.getActiveGroupMembers(userId).stream()
                .map(groupMember -> {
                    Group group = groupRepository.findById(groupMember.getGroupId())
                            .orElseThrow();
                    GroupChallenge groupChallenge = groupChallengeService.getLatestChallenge(groupMember.getGroupId());
                    List<GroupMemberResponse> members = groupMemberService.getGroupMembers(groupMember.getGroupId());

                    return toGroupResponse(group, groupMember, groupChallenge, members);
                })
                .toList();
    }

    public GroupResponse getGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."));
        GroupMember currentMember = getAccessibleGroupMember(userId, groupId);
        GroupChallenge groupChallenge = groupChallengeService.getLatestChallenge(groupId);
        List<GroupMemberResponse> members = groupMemberService.getGroupMembers(groupId);

        return toGroupResponse(group, currentMember, groupChallenge, members);
    }

    public GroupResponse updateGroup(Long groupId, Long userId, String name) {
        throw new UnsupportedOperationException("아직 미구현 - API 문서화 단계");
    }

    public void withdrawGroup(Long groupId, Long userId) {
        throw new UnsupportedOperationException("아직 미구현 - API 문서화 단계");
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."));
        List<Long> groupChallengeIds = groupChallengeService.getGroupChallenges(groupId).stream()
                .map(GroupChallenge::getId)
                .toList();

        groupChallengeParticipantService.deleteGroupChallengeParticipants(groupChallengeIds);
        groupChallengeService.deleteGroupChallenges(groupId);
        groupMemberService.deleteGroupMembers(groupId);
        groupRepository.delete(group);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        lockUserForGroupOperation(userId);

        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."));

        GroupMember groupMember = groupMemberService.findActiveGroupMember(userId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 속한 그룹만 탈퇴할 수 있습니다."));
        GroupChallenge latestChallenge = groupChallengeService.getLatestChallenge(groupId);

        groupMemberService.leaveGroupMember(groupMember);
        groupChallengeParticipantService.withdrawGroupChallengeParticipant(latestChallenge.getId(), groupMember.getId());
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

    private GroupMember getAccessibleGroupMember(Long userId, Long groupId) {
        try {
            return groupMemberService.getActiveGroupMember(userId, groupId);
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 속한 그룹만 조회할 수 있습니다.");
        }
    }

    private void lockUserForGroupOperation(Long userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Group saveGroupWithUniqueInviteCode(String groupName) {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_GENERATION_ATTEMPTS; attempt++) {
            String inviteCode = inviteCodeGenerator.generate();

            try {
                return saveGroup(groupName, inviteCode);
            } catch (DataIntegrityViolationException exception) {
                // `invite_code` 유일성은 DB가 보장하므로, 중복 충돌이 나면 여기서 재시도한다.
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용 가능한 초대코드를 생성할 수 없습니다.");
    }
}
