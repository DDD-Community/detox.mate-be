package com.detoxmate.group.controller;

import com.detoxmate.group.dto.GroupChallengeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GroupChallengeController {

    @GetMapping("/me/group-challenges")
    public List<GroupChallengeResponse> getMyGroupChallenges(
            @RequestParam(required = false) String status
    ) {
        return GroupMockData.myGroupChallengesResponse(status);
    }

    @GetMapping("/group-challenges/{id}")
    public GroupChallengeResponse getGroupChallenge(@PathVariable long id) {
        return GroupMockData.groupChallengeDetailResponse(id);
    }
}
