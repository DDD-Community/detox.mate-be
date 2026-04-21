package com.detoxmate.group.service;

import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupChallengeParticipantService {
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository;

    public GroupChallengeParticipant saveGroupChallengeParticipant(Long groupMemberId, Long groupChallengeId) {
        return groupChallengeParticipantRepository.save(GroupChallengeParticipant.join(groupMemberId, groupChallengeId));
    }

    public void deleteGroupChallengeParticipants(List<Long> groupChallengeIds) {
        if (groupChallengeIds.isEmpty()) {
            return;
        }

        groupChallengeParticipantRepository.deleteAllByGroupChallengeIdIn(groupChallengeIds);
    }

    public void withdrawGroupChallengeParticipant(Long groupChallengeId, Long groupMemberId) {
        groupChallengeParticipantRepository.findByGroupChallengeIdAndGroupMemberId(groupChallengeId, groupMemberId)
                .ifPresent(GroupChallengeParticipant::withdraw);
    }
}
