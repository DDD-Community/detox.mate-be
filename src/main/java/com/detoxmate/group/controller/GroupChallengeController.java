package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.group.dto.GroupChallengeResponse;
import com.detoxmate.group.service.GroupChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupChallengeController {
    private final GroupChallengeService groupChallengeService;

    @GetMapping("/me/group-challenges")
    public List<GroupChallengeResponse> getMyGroupChallenges(
            CurrentUser currentUser,
            @RequestParam(required = false) String status
    ) {
        return groupChallengeService.getMyGroupChallenges(currentUser.id(), status);
    }

    @GetMapping("/group-challenges/{id}")
    public GroupChallengeResponse getGroupChallenge(
            CurrentUser currentUser,
            @PathVariable long id
    ) {
        return groupChallengeService.getGroupChallenge(id, currentUser.id());
    }
}
