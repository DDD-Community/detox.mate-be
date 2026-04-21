package com.detoxmate.user.controller;

import com.detoxmate.common.AccessTokenExtractor;
import com.detoxmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserDevController {
    private final UserService userService;

    @DeleteMapping("/me")
    public void withdraw(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String accessToken = AccessTokenExtractor.require(authorizationHeader);
        userService.withdraw(accessToken);
    }
}
