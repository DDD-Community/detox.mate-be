package com.detoxmate.group.service;

import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupChallengeParticipantService {
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository;

    public GroupChallengeParticipant saveGroupChallengeParticipant(Long groupMemberId, Long groupChallengeId) {
        return groupChallengeParticipantRepository.save(GroupChallengeParticipant.join(groupMemberId, groupChallengeId));
    }
}
