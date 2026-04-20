package com.detoxmate.group.service;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeStatus;
import com.detoxmate.group.dto.GroupChallengeParticipantRow;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import com.detoxmate.group.dto.GroupChallengeResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class GroupChallengeService {
    private final GroupChallengeRepository groupChallengeRepository;
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository;
    private final GroupRepository groupRepository;

    public GroupChallenge saveGroupChallenge(Long groupId) {
        return groupChallengeRepository.save(GroupChallenge.createFirst(groupId));
    }

    public GroupChallenge getLatestChallenge(Long groupId) {
        return groupChallengeRepository.findTopByGroupIdOrderByChallengeNoDesc(groupId)
                .orElseThrow();
    }

    public List<GroupChallengeResponse> getMyGroupChallenges(Long userId, String status) {
        GroupChallengeStatus groupChallengeStatus = parseStatus(status);
        List<GroupChallenge> groupChallenges =
                groupChallengeRepository.findAllByParticipantUserIdAndStatus(userId, groupChallengeStatus);

        if (groupChallenges.isEmpty()) {
            return List.of();
        }

        // 목록 응답은 챌린지 수만큼 다시 조회하지 않고, 그룹명/참가자를 배치로 모아 한 번씩만 읽는다.
        Map<Long, String> groupNamesById = getGroupNamesById(groupChallenges);
        Map<Long, List<GroupChallengeParticipantResponse>> participantsByChallengeId =
                getParticipantsByChallengeId(groupChallenges);

        return groupChallenges.stream()
                .map(groupChallenge -> toGroupChallengeResponse(
                        groupChallenge,
                        groupNamesById.get(groupChallenge.getGroupId()),
                        participantsByChallengeId.getOrDefault(groupChallenge.getId(), List.of())
                ))
                .toList();
    }

    public GroupChallengeResponse getGroupChallenge(Long groupChallengeId, Long userId) {
        GroupChallenge groupChallenge = groupChallengeRepository.findById(groupChallengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹 챌린지를 찾을 수 없습니다."));

        if (!groupChallengeParticipantRepository.existsByGroupChallengeIdAndUserId(groupChallengeId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "참가한 그룹 챌린지만 조회할 수 있습니다.");
        }

        return toGroupChallengeResponse(
                groupChallenge,
                getGroupName(groupChallenge.getGroupId()),
                getParticipants(groupChallenge.getId())
        );
    }

    private GroupChallengeResponse toGroupChallengeResponse(
            GroupChallenge groupChallenge,
            String groupName,
            List<GroupChallengeParticipantResponse> participants
    ) {
        return new GroupChallengeResponse(
                groupChallenge.getId(),
                groupChallenge.getGroupId(),
                groupName,
                groupChallenge.getChallengeNo(),
                groupChallenge.getStatus().name(),
                participants,
                groupChallenge.getStartAt(),
                groupChallenge.getEndAt(),
                groupChallenge.getCreatedAt(),
                groupChallenge.getUpdatedAt()
        );
    }

    private Map<Long, String> getGroupNamesById(List<GroupChallenge> groupChallenges) {
        List<Long> groupIds = groupChallenges.stream()
                .map(GroupChallenge::getGroupId)
                .distinct()
                .toList();

        Map<Long, String> groupNamesById = groupRepository.findAllById(groupIds).stream()
                .collect(toMap(Group::getId, Group::getName));

        for (Long groupId : groupIds) {
            if (!groupNamesById.containsKey(groupId)) {
                throw new IllegalStateException("챌린지에 연결된 그룹을 찾을 수 없습니다.");
            }
        }

        return groupNamesById;
    }

    private String getGroupName(Long groupId) {
        return groupRepository.findById(groupId)
                .map(Group::getName)
                .orElseThrow(() -> new IllegalStateException("챌린지에 연결된 그룹을 찾을 수 없습니다."));
    }

    private Map<Long, List<GroupChallengeParticipantResponse>> getParticipantsByChallengeId(List<GroupChallenge> groupChallenges) {
        List<Long> challengeIds = groupChallenges.stream()
                .map(GroupChallenge::getId)
                .toList();

        return groupChallengeParticipantRepository.findParticipantRowsByGroupChallengeIds(challengeIds).stream()
                .collect(groupingBy(
                        GroupChallengeParticipantRow::groupChallengeId,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(this::toParticipantResponse, java.util.stream.Collectors.toList())
                ));
    }

    private List<GroupChallengeParticipantResponse> getParticipants(Long groupChallengeId) {
        return groupChallengeParticipantRepository.findParticipantResponsesByGroupChallengeId(groupChallengeId).stream()
                .map(this::withEmptyGoalTimes)
                .toList();
    }

    private GroupChallengeParticipantResponse toParticipantResponse(GroupChallengeParticipantRow participantRow) {
        return new GroupChallengeParticipantResponse(
                participantRow.id(),
                participantRow.groupMemberId(),
                participantRow.userId(),
                participantRow.displayName(),
                participantRow.profileImageUrl(),
                participantRow.status(),
                participantRow.joinedAt(),
                participantRow.withdrawnAt(),
                List.of()
        );
    }

    private GroupChallengeParticipantResponse withEmptyGoalTimes(GroupChallengeParticipantResponse participant) {
        return new GroupChallengeParticipantResponse(
                participant.id(),
                participant.groupMemberId(),
                participant.userId(),
                participant.displayName(),
                participant.profileImageUrl(),
                participant.status(),
                participant.joinedAt(),
                participant.withdrawnAt(),
                List.of()
        );
    }

    private GroupChallengeStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return GroupChallengeStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 상태값입니다.");
        }
    }
}
