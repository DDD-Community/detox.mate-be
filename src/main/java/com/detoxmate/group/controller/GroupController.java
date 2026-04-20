package com.detoxmate.group.controller;

import com.detoxmate.common.AccessTokenExtractor;
import com.detoxmate.group.dto.CreateGroupRequest;
import com.detoxmate.group.dto.GroupResponse;
import com.detoxmate.group.dto.JoinGroupRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GroupController {

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        AccessTokenExtractor.require(authorizationHeader);
        return GroupMockData.createGroupResponse(request.name());
    }

    @PostMapping("/groups/join")
    public GroupResponse joinGroup(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody JoinGroupRequest request
    ) {
        AccessTokenExtractor.require(authorizationHeader);
        return GroupMockData.joinGroupResponse(request.inviteCode());
    }

    @GetMapping("/me/groups")
    public List<GroupResponse> getMyGroups(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        AccessTokenExtractor.require(authorizationHeader);
        return GroupMockData.myGroupsResponse();
    }

    @GetMapping("/groups/{id}")
    public GroupResponse getGroup(@PathVariable long id) {
        return GroupMockData.groupDetailResponse(id);
    }
}
