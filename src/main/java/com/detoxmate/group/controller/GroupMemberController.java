package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.group.dto.GroupMemberProfileResponse;
import com.detoxmate.group.service.GroupMemberProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupMemberProfileService groupMemberProfileService;

    @GetMapping("/groups/{groupId}/members/{memberId}")
    public GroupMemberProfileResponse getGroupMemberProfile(
            CurrentUser currentUser,
            @PathVariable("groupId") Long groupId,
            @PathVariable("memberId") Long memberId
    ) {
        return groupMemberProfileService.getGroupMemberProfile(groupId, memberId, currentUser.id());
    }
}
