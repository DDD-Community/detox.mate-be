package com.detoxmate.friend.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.friend.dto.CreateFriendRequest;
import com.detoxmate.friend.dto.FriendInviteResponse;
import com.detoxmate.friend.dto.FriendRequestResponse;
import com.detoxmate.friend.dto.FriendResponse;
import com.detoxmate.friend.dto.FriendInviteeResponse;
import com.detoxmate.friend.dto.FriendReceivedRequestResponse;
import com.detoxmate.friend.dto.FriendSearchResponse;
import com.detoxmate.friend.service.FriendService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/invite")
    public FriendInviteResponse getMyInvite(CurrentUser currentUser) {
        return friendService.getMyInvite(currentUser.id());
    }

    @GetMapping("/invite/{code}")
    public FriendInviteeResponse getInvitee(
            CurrentUser currentUser,
            @PathVariable String code
    ) {
        return friendService.getInvitee(code, currentUser.id());
    }

    @GetMapping("/search")
    public FriendSearchResponse searchByEmail(
            CurrentUser currentUser,
            @RequestParam("email") @NotBlank @Email String email
    ) {
        return friendService.searchByEmail(email, currentUser.id());
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestResponse sendRequest(
            CurrentUser currentUser,
            @Valid @RequestBody CreateFriendRequest request
    ) {
        return friendService.sendRequest(currentUser.id(), request.targetUserId());
    }

    @GetMapping("/requests/sent")
    public List<FriendRequestResponse> getSentRequests(CurrentUser currentUser) {
        return friendService.getSentRequests(currentUser.id());
    }

    @GetMapping("/requests/received")
    public List<FriendReceivedRequestResponse> getReceivedRequests(CurrentUser currentUser) {
        return friendService.getReceivedRequests(currentUser.id());
    }

    @PostMapping("/requests/{requestId}/accept")
    public FriendResponse acceptRequest(
            CurrentUser currentUser,
            @PathVariable Long requestId
    ) {
        return friendService.acceptRequest(requestId, currentUser.id());
    }

    @DeleteMapping("/requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePendingRequest(
            CurrentUser currentUser,
            @PathVariable Long requestId
    ) {
        friendService.deletePendingRequest(requestId, currentUser.id());
    }

    @GetMapping
    public List<FriendResponse> getFriends(CurrentUser currentUser) {
        return friendService.getFriends(currentUser.id());
    }

    @DeleteMapping("/{friendshipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfriend(
            CurrentUser currentUser,
            @PathVariable Long friendshipId
    ) {
        friendService.unfriend(friendshipId, currentUser.id());
    }
}
