package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequiredArgsConstructor
public class GroupDevController {

    private final GroupService groupService;

    @DeleteMapping("/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(
            CurrentUser currentUser,
            @PathVariable long id
    ) {
        groupService.deleteGroup(id);
    }
}
