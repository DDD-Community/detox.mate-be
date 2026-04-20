package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.group.dto.CreateGroupRequest;
import com.detoxmate.group.dto.GroupResponse;
import com.detoxmate.group.dto.JoinGroupRequest;
import com.detoxmate.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
            CurrentUser currentUser,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return groupService.createGroup(currentUser.id(), request.name());
    }

    @PostMapping("/groups/join")
    public GroupResponse joinGroup(
            CurrentUser currentUser,
            @Valid @RequestBody JoinGroupRequest request
    ) {
        return groupService.joinGroup(request.inviteCode(), currentUser.id());
    }

    @GetMapping("/me/groups")
    public List<GroupResponse> getMyGroups(
            CurrentUser currentUser
    ) {
        return groupService.getMyGroups(currentUser.id());
    }

    @GetMapping("/groups/{id}")
    public GroupResponse getGroup(
            CurrentUser currentUser,
            @PathVariable long id
    ) {
        return groupService.getGroup(id, currentUser.id());
    }
}
