package com.detoxmate.firstscreentime.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.firstscreentime.dto.FirstScreenTimeCreateRequest;
import com.detoxmate.firstscreentime.dto.FirstScreenTimeResponse;
import com.detoxmate.firstscreentime.service.FirstScreenTimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FirstScreenTimeController {

    private final FirstScreenTimeService firstScreenTimeService;

    @PostMapping("/first-screen-times")
    @ResponseStatus(HttpStatus.CREATED)
    public FirstScreenTimeResponse create(
            CurrentUser currentUser,
            @Valid @RequestBody FirstScreenTimeCreateRequest request
    ) {
        return firstScreenTimeService.create(currentUser.id(), request);
    }

    @GetMapping("/group-challenge-participants/{groupChallengeParticipantId}/first-screen-time")
    public FirstScreenTimeResponse get(
            CurrentUser currentUser,
            @PathVariable Long groupChallengeParticipantId
    ) {
        return firstScreenTimeService.get(currentUser.id(), groupChallengeParticipantId);
    }
}
