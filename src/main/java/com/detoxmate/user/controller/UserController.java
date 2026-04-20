package com.detoxmate.user.controller;

import com.detoxmate.common.AccessTokenExtractor;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public MyProfileResponse getMe(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String accessToken = AccessTokenExtractor.require(authorizationHeader);
        return userService.getMe(accessToken);
    }

    @DeleteMapping("/me")
    public void withdraw(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String accessToken = AccessTokenExtractor.require(authorizationHeader);
        userService.withdraw(accessToken);
    }
}
