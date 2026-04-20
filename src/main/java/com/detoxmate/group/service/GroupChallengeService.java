package com.detoxmate.group.service;

import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeStatus;
import com.detoxmate.group.repository.GroupChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupChallengeService {
    private final GroupChallengeRepository groupChallengeRepository;

    public GroupChallenge saveGroupChallenge(Long groupId) {
        return groupChallengeRepository.save(GroupChallenge.createFirst(groupId));
    }

    public GroupChallenge getLatestChallenge(Long groupId) {
        return groupChallengeRepository.findTopByGroupIdOrderByChallengeNoDesc(groupId)
                .orElseThrow();
    }

}
