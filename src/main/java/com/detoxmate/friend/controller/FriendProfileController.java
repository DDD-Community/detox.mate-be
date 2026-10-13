package com.detoxmate.friend.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.friend.dto.FriendProfileResponse;
import com.detoxmate.friend.service.FriendProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendProfileController {

    private final FriendProfileService friendProfileService;

    @GetMapping("/{friendUserId}/profile")
    public FriendProfileResponse getProfile(CurrentUser currentUser, @PathVariable Long friendUserId) {
        return friendProfileService.getProfile(currentUser.id(), friendUserId);
    }
}
